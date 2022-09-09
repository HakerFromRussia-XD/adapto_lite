#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <strings.h>
#include <errno.h>
#include "lwip/opt.h"
#include "lwip/tcp.h"
#include "lwip/udp.h"
#include "lwip/mem.h"
#include "dnsproxy.h"
#include "eth_router.h"
#include "eth_queue.h"
#include "lwip/lwip.h"
#include "eth_nat.h"
#include "linux/tcp.h"

#define PROT_TCP 6
#define PROT_UDP 17
#define PROT_ICMP 1

#define NAT_DIVERT_PORT 1234
pthread_t tnatHandle;
pthread_t tnatRXHandle;
//static struct NHandle_t tNotifyHandle;
#define taskNOTIFY_NEW_DATA 1


// src ip:port / dst ip:port -> src ip/port / dst <- divert ip:port
// В пакете подменяется dst ip:port, и в таблицу nat пишется соответствие src ip:port к dst ip:port

struct NAT_TAB nattab[NAT_TAB_SIZE];

struct NAT_STAT nat_stat;

static int nat_task_pipe[2];
static int nat_rx_task_pipe[2];


_Noreturn static void calc_stats(void * arg) {
while(1) {
    int nofl_conn = 0;
    for (int i = 0; i < NAT_TAB_SIZE; i++) {
        int didx=nattab[i].index_end-nattab[i].index_begin;
        if (didx<0) didx+=SNDQ_MAXLEN;
        nofl_conn+=didx;
//        if (nattab[i].index_begin != nattab[i].index_end) nofl_conn++;
    }
    nat_stat.lw_conn_used = nofl_conn;
    usleep(10000);
}
}


int forward_nat(struct ETH_QUEUE* block) {// пакет в интернет - заворачивается на локальный ip/port по таблице соответствий
  struct ETH_PKT_HDR* pkt=(struct ETH_PKT_HDR*)block->pbuf->pbuf.payload;
  if (pkt->ethertype!=htons(0x800)) return -1;// no match
  if (pkt->ip_dst==0xffffffff) return -1;//broadcast
  if ((gnetif.ip_addr.addr & gnetif.netmask.addr) == (pkt->ip_dst & gnetif.netmask.addr) ) return -1;// local dgram
  if (pkt->proto!=PROT_TCP && pkt->proto!=PROT_UDP/* && pkt->proto!=PROT_ICMP*/) return -1;

  uint32_t curtime=xTaskGetTickCount();

  uint8_t* ip=&pkt->ip_src;
  uint16_t port=htons(pkt->port_src);
  uint8_t* dip=&pkt->ip_dst;
  uint16_t dport=htons(pkt->port_dst);

  int nat_entry=-1;
  for (int i=0; i<NAT_TAB_SIZE; i++) {
//    if (pkt->proto==PROT_TCP || pkt->proto==PROT_UDP) {
      if (pkt->ip_src == nattab[i].src_ip &&
          pkt->ip_dst==nattab[i].dst_ip &&
          pkt->port_src==nattab[i].src_port &&
          pkt->port_dst==nattab[i].dst_port &&
          nattab[i].proto==pkt->proto ) { /*print_log("[NAT_TCP] Found match...");*/ nat_entry=i; break;}
//    }
  }
  if (nat_entry==-1) {// find oldest entry
    int oldent=0;
    uint32_t maxtime=0;
    for (int i=0; i<NAT_TAB_SIZE; i++) {
        if (nattab[i].status) continue;// не трогаем не разорванные TCP соединения
      if (curtime-nattab[i].time>maxtime) { maxtime=curtime-nattab[i].time; oldent=i; }
    }
    nat_entry=oldent;
    nattab[nat_entry].src_ip=pkt->ip_src;
    nattab[nat_entry].dst_ip=pkt->ip_dst;
    nattab[nat_entry].src_port=pkt->port_src;
    nattab[nat_entry].dst_port=pkt->port_dst;
    nattab[nat_entry].proto=pkt->proto;
  }
  nattab[nat_entry].time=curtime;
  pkt->ip_dst=gnetif.ip_addr.addr;
  pkt->port_dst=htons(NAT_DIVERT_PORT);

//  print_log("[NAT_TCP] === forward pkt from %d.%d.%d.%d:%d to %d.%d.%d.%d:%d nat_entry %d proto %d", ip[0],ip[1],ip[2],ip[3], port, dip[0],dip[1],dip[2],dip[3], dport, nat_entry, pkt->proto);


  return 0;
  }

int backward_nat(struct ETH_QUEUE* block) {// пакет из интернета (т.е. LWIP стека) в локалку
  struct ETH_PKT_HDR *pkt = (struct ETH_PKT_HDR *) block->pbuf->pbuf.payload;
  if (pkt->ethertype != htons(0x800)) return -1;// no match
  if (pkt->ip_src != gnetif.ip_addr.addr && pkt->port_src != htons(NAT_DIVERT_PORT))
    return -1;// local dgram
  if (pkt->proto != PROT_TCP && pkt->proto != PROT_UDP && pkt->proto != PROT_ICMP) return -1;

  uint32_t curtime = xTaskGetTickCount();

  int nat_entry = -1;
  for (int i = 0; i < NAT_TAB_SIZE; i++) {
    if (pkt->ip_dst == nattab[i].src_ip && pkt->port_dst == nattab[i].src_port &&
        pkt->proto == nattab[i].proto) {
      nat_entry = i;
      break;
    }
  }
  if (nat_entry == -1) return -1;// no match
  pkt->ip_src = nattab[nat_entry].dst_ip;
  pkt->port_src = nattab[nat_entry].dst_port;

  uint8_t* ip=&pkt->ip_src;
  uint16_t port=htons(pkt->port_src);
  uint8_t* dip=&pkt->ip_dst;
  uint16_t dport=htons(pkt->port_dst);

//  print_log("[NAT_TCP] === backward pkt from %d.%d.%d.%d:%d to %d.%d.%d.%d:%d nat_entry %d proto %d", ip[0],ip[1],ip[2],ip[3], port, dip[0],dip[1],dip[2],dip[3], dport, nat_entry, pkt->proto);

  return 0;
  }

struct tcp_pcb* nat_tcp;

static void nat_close_so_conn(uint32_t idx) {
    if (nattab[idx].status) {
      shutdown(nattab[idx].socket, SHUT_RDWR);
      nattab[idx].status=0;
      }
//    if (nattab[idx].status) { close(nattab[idx].socket); nattab[idx].status=0;}
}

static void nat_close_lw_conn(uint32_t idx) {
    if (nattab[idx].pcb) {
      uint32_t ptr=nattab[idx].index_begin;
      while (ptr!=nattab[idx].index_end) {
        if (nattab[idx].p[ptr]) {
            tcp_recved(nattab[idx].pcb, nattab[idx].p[ptr]->tot_len);
            pbuf_free(nattab[idx].p[ptr]);
            nat_stat.pbuf_used--;
            nattab[idx].p[ptr]=NULL;
        }
        ptr++;
        if (ptr>=SNDQ_MAXLEN) ptr=0;
        }
      nattab[idx].offs=0;
      nattab[idx].index_begin=ptr;
      nat_stat.tcp_close++;
      err_t e=tcp_shutdown(nattab[idx].pcb, 1, 1);
      if (e==ERR_OK) {
          tcp_abort(nattab[idx].pcb);
          nattab[idx].pcb=NULL;
        }
        nattab[idx].pcb=NULL;
      }
    nattab[idx].txbuf_len=0;
}

_Noreturn static void NAT_Task( void* arg ) {// здесь обслуживаем передачу из LWIP в сокет
  uint32_t sel_req=0;
  LOCK_LWIP_CORE();
  while (1) {
    uint32_t act;

      int maxfd=-1;
      fd_set wfd;
      FD_ZERO(&wfd);
      for (int i = 0; i < NAT_TAB_SIZE; i++) {
        if (nattab[i].status>0 ) {// descriptor is active
          if (nattab[i].socket>maxfd) maxfd=nattab[i].socket;
          if (nattab[i].index_begin!=nattab[i].index_end) { FD_SET(nattab[i].socket, &wfd); sel_req=1; }
        }
      }

      fd_set rfd;
      FD_ZERO(&rfd);
      FD_SET(nat_task_pipe[0], &rfd);
      if (maxfd<nat_task_pipe[0]) maxfd=nat_task_pipe[0];

      UNLOCK_LWIP_CORE();
//      struct timeval timeout={0,100*1000};// x0.1s потом ожно добавить еще один локальный сокет чтобы будить (добавить поток)
      int res = select(maxfd+1, &rfd, &wfd, NULL, NULL /*&timeout*/);
      LOCK_LWIP_CORE();
      if (res == -1) continue;

      if (FD_ISSET(nat_task_pipe[0], &rfd)) {// new socket
//        print_log("[NAT_Task] pipe event");
        char dummy;
        read(nat_task_pipe[0], &dummy, 1);// recv from socket
        }

      for (int i = 0; i < NAT_TAB_SIZE; i++) {
        if (nattab[i].status==0) continue;// дескриптор куда-то делся
        if (FD_ISSET(nattab[i].socket, &wfd)) do {// socket ready for write data
          
//            print_log("[NAT_Task] Write to tab %d", i);

            struct pbuf *p=nattab[i].p[nattab[i].index_begin];
            struct pbuf *p1=p;
            uint32_t offs=nattab[i].offs;
            int size=p->tot_len;
            while(p1->next!=NULL && offs>=p1->len) {// SEEK to offs into pbuf chain
              if (p1->next==NULL && p1->len!=size) { nat_stat.pbuf_broken++; size=p1->len; }
              offs-=p1->len; size-=p1->len; p1=p->next;
            }
            int len=send(nattab[i].socket, (uint8_t*)p1->payload+offs, p->len-offs, MSG_DONTWAIT);
//            print_log("[NAT_Task] tab %d send %d:%d bytes, %d sent", i, offs, p->len-offs, len);
            if (len<0) {
              if (errno == EAGAIN || errno == ENOBUFS) break;// 
              print_log("[NAT_Task] tab %d send error, close connection (err %d: %s)", i, errno, strerror(errno));
              nattab[i].shutdown_req=1;
              nat_close_so_conn(i);
              nat_close_lw_conn(i);
              break;
              }
            nattab[i].offs+=len;
            if (nattab[i].offs<p->tot_len) { sel_req=1; break;}// not all sent

            nattab[i].p[nattab[i].index_begin]=0;
            nattab[i].index_begin++;
            if (nattab[i].index_begin>=SNDQ_MAXLEN) nattab[i].index_begin=0;
            nattab[i].offs=0;
            tcp_recved(nattab[i].pcb, p->tot_len);
            nat_stat.pbuf_used--;
            pbuf_free(p);
//            print_log("[NAT_Task] tab %d free pbuf", i);
        } while(0);
      }
  }
}

_Noreturn static void NAT_RX_Task( void* arg ) {// здесь обслуживаем прием из сокетов и передачу в LWIP
  uint32_t sel_req=0;
  LOCK_LWIP_CORE();
  while (1) {
    uint32_t act;

      int maxfd=-1;
      fd_set rfd, efd;
      FD_ZERO(&rfd); FD_ZERO(&efd);
      for (int i = 0; i < NAT_TAB_SIZE; i++) {
        if (nattab[i].status>0 ) {// descriptor is active
          if (nattab[i].socket>maxfd) maxfd=nattab[i].socket;
          FD_SET(nattab[i].socket, &rfd);
          FD_SET(nattab[i].socket, &efd);
        }
      }
      FD_SET(nat_rx_task_pipe[0], &rfd);
      if (maxfd<nat_rx_task_pipe[0]) maxfd=nat_rx_task_pipe[0];
      if (maxfd==-1) {
        UNLOCK_LWIP_CORE();
        usleep(100*1000);
        LOCK_LWIP_CORE();
        continue;
        }
      UNLOCK_LWIP_CORE();
//      struct timeval timeout={0,100*1000};// x0.1s потом ожно добавить еще один локальный сокет чтобы будить (добавить поток)
      int res = select(maxfd+1, &rfd, NULL, &efd, NULL /*&timeout*/);
      LOCK_LWIP_CORE();
      if (res == -1) continue;

      if (FD_ISSET(nat_rx_task_pipe[0], &rfd)) {// new socket
//        print_log("[NAT_rxTask] pipe event");
        char dummy;
        read(nat_rx_task_pipe[0], &dummy, 1);// recv from socket
        }

      for (int i = 0; i < NAT_TAB_SIZE; i++) {
        if (nattab[i].status==0) continue;// дескриптор куда-то делся
        if (FD_ISSET(nattab[i].socket, &efd)) {// error: close connection
            print_log("[NAT_rxTask] tab %d select errorfd occurs, close connection (by remote host)", i);
            nattab[i].shutdown_req=1;
            nat_close_so_conn(i);
            nat_close_lw_conn(i);
            continue;// отключили сокет
        }
        if (FD_ISSET(nattab[i].socket, &rfd)) {// socket ready for read data
          int rfd_cont=0;
          do {
            if (nattab[i].txbuf_len <= 0) {
              int len = recv(nattab[i].socket, nattab[i].txbuf, sizeof(nattab[0].txbuf), MSG_DONTWAIT);// recv from socket
//              print_log("[NAT_rxTask] tab %d recv %d bytes err %d %s", i, len, errno, strerror(errno));
              if (len==0 && rfd_cont==0) {// connection shutdown request
                print_log("[NAT_rxTask] tab %d shutdown connection", i);
                nattab[i].shutdown_req=1;
                nat_close_so_conn(i);
                nat_close_lw_conn(i);
                break;
                }
              rfd_cont++;
              if (len<0) {
                if (errno == EAGAIN) break;//
                print_log("[NAT_rxTask] tab %d recv error, close connection (err %d: %s)", i, errno, strerror(errno));
                nattab[i].shutdown_req=1;
                nat_close_so_conn(i);
                nat_close_lw_conn(i);
                }
              if (len < 1) { break; }// error recv data
              nattab[i].txbuf_len=len;
            }
            if (nattab[i].txbuf_len > 0) {
              err_t err=tcp_write(nattab[i].pcb, nattab[i].txbuf, nattab[i].txbuf_len, TCP_WRITE_FLAG_COPY);// write to lwip
              if (err==ERR_OK) {
                nattab[i].txbuf_len=0;
                continue;
              }
            }
            break;
          } while(1);
        }
      }
  }
}

static void nat_tcp_err(void *arg, err_t err) { // The pcb had an error and is already deallocated. The argument might still be valid (if != NULL).
    if (arg) {
        uint32_t num=(int)arg-1;
        print_log("[NAT_Task] tab %d close connection (by local host)", num);
        if (num<NAT_TAB_SIZE) {
          nat_close_so_conn(num);
          nattab[num].pcb=NULL;
          }
    }
}


static int nat_find_match(uint32_t ip, uint16_t port, uint8_t proto) {
  for (int i = 0; i < NAT_TAB_SIZE; i++) {
    if (ip == nattab[i].src_ip && port == nattab[i].src_port && proto == nattab[i].proto) return i;
  }
  return -1;
}

static err_t nat_tcp_sent(void *arg, struct tcp_pcb *pcb, u16_t len) {// Data has been sent and acknowledged by the remote host. This means that more data can be sent.
//  TNotifyAdd( &tNotifyHandle, taskNOTIFY_NEW_DATA);
  write(nat_task_pipe[1], "1",1);// notify for new data
  return ERR_OK;
}


static err_t nat_tcp_recv(void *arg, struct tcp_pcb *pcb, struct pbuf *p, err_t err) { // Data has been received on this pcb.
    uint32_t nat_entry=(int)arg-1;
    if ((err != ERR_OK) || (pcb == NULL) || (p == NULL)) {
      print_log("[NAT_LWTCP_RECV1] closing connection by loacl side request: err=%d pcb=%d p=%d", err, pcb, p);
      if (nat_entry<NAT_TAB_SIZE) {
        nattab[nat_entry].shutdown_req=1;
        nat_close_so_conn(nat_entry);
        nat_close_lw_conn(nat_entry);
        }
      return ERR_VAL;
      }
    uint32_t* ip=&pcb->remote_ip.addr;
    uint16_t port=htons(pcb->remote_port);
//    print_log("[NAT_LWTCP_RECV3] nat_entry=%d rport=%d %d.%d.%d.%d", nat_entry, htons(port), (*ip)&0xff, (*ip>>8)&0xff, (*ip>>16)&0xff, (*ip>>24)&0xff);
    if (nat_entry>=NAT_TAB_SIZE) goto recv_abrt;
    if (*ip!=nattab[nat_entry].src_ip || port != nattab[nat_entry].src_port || nattab[nat_entry].proto != PROT_TCP) {nattab[nat_entry].errors++; goto recv_abrt; }
    uint16_t idx=nattab[nat_entry].index_end;
    if (idx>=SNDQ_MAXLEN) { nattab[nat_entry].errors++; goto recv_abrt; }
    int idx_next=idx+1;
    if (idx_next>=SNDQ_MAXLEN) idx_next=0;
    if (idx_next==nattab[nat_entry].index_begin) { nattab[nat_entry].rx_ovf++; print_log("[NAT_LWTCP_RECV] RX_OVF: idx_b=%d idx_e=%d", nattab[nat_entry].index_begin, nattab[nat_entry].index_end ); goto recv_abrt; }
//    print_log("[NAT_TCP] tab %d LWIP RECV %d/%d bytes", nat_entry, p->len, p->tot_len);

    if (nattab[nat_entry].pcb!=pcb) { print_log("[NAT_TCP] PCB not match!!!!!!!!!!!!!!!!"); while(1);}
    nattab[nat_entry].p[idx]=p;
    nattab[nat_entry].index_end=idx_next;
    nat_stat.pbuf_used++;
//    TNotifyAdd( &tNotifyHandle, taskNOTIFY_NEW_DATA);
    write(nat_task_pipe[1], "1",1);// notify for new data

//    nattab[nat_entry].shutdown_req=1;
//    nat_close_so_conn(nat_entry);
//    nat_close_lw_conn(nat_entry);

    return ERR_OK;
    recv_abrt:
    if (p) {
        tcp_recved(pcb, p->tot_len);
        pbuf_free(p);
    }
    return ERR_OK;

//    return ERR_ABRT;
}

static err_t nat_accept_fn(void *arg, struct tcp_pcb *pcb, err_t err) {
//    print_log("[NAT_TCP] === NEW connection callback");

    if ((err != ERR_OK) || (pcb == NULL)) { return ERR_VAL; }
    uint8_t* ip=(uint8_t*)&pcb->remote_ip.addr;
    uint16_t port=pcb->remote_port;
    print_log("[NAT_TCP] === NEW connecion from %d.%d.%d.%d:%d", ip[0],ip[1],ip[2],ip[3], port);
    int entry=nat_find_match(pcb->remote_ip.addr, htons(pcb->remote_port), PROT_TCP);
    if (entry<0) return ERR_VAL;
    tcp_arg(pcb, (void*)(entry+1));// arg for this TCP connection
    int s=socket(AF_INET, SOCK_STREAM, 0);
    if (s==-1) { print_log("[NAT_TCP] tab %d socket allocation err", entry); return ERR_VAL; }
    int status = fcntl(s, F_SETFL, fcntl(s, F_GETFL, 0) | O_NONBLOCK);
    if (status==-1) { close(s); print_log("[NAT_TCP] tab %d FCNTL error", entry); return ERR_VAL; }
    int keepcnt = 8;
    int keepidle = 2;
    int keepintvl = 2;
    int nodelay = 1;
    int syncnt = 3;
    setsockopt(s, IPPROTO_TCP, TCP_KEEPCNT, &keepcnt, sizeof(int));
    setsockopt(s, IPPROTO_TCP, TCP_KEEPIDLE, &keepidle, sizeof(int));
    setsockopt(s, IPPROTO_TCP, TCP_KEEPINTVL, &keepintvl, sizeof(int));
    setsockopt(s, IPPROTO_TCP, TCP_NODELAY, &nodelay, sizeof(int));
    setsockopt(s, IPPROTO_TCP, TCP_SYNCNT, &syncnt, sizeof(int));
    tcp_nagle_disabled(pcb);

    struct sockaddr_in servaddr;
    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = nattab[entry].dst_ip;
    servaddr.sin_port = nattab[entry].dst_port;
    ip=(uint8_t*)&servaddr.sin_addr.s_addr;
    port=htons(servaddr.sin_port);
    print_log("[NAT_TCP] === connecion to %d.%d.%d.%d:%d", ip[0],ip[1],ip[2],ip[3], port);
    int r=connect(s, (struct sockaddr*)&servaddr, sizeof(servaddr));
    if (r != 0) if (errno != EINPROGRESS)
      {
      print_log("[NAT_TCP] tab %d can not connect: %d", entry, errno);
      close(s);
      return ERR_VAL;
      }
    nattab[entry].socket=s;
    nattab[entry].status=1;
    nattab[entry].index_begin=0;
    nattab[entry].index_end=0;
    nattab[entry].offs=0;
    nattab[entry].shutdown_req=0;
    nattab[entry].pcb=pcb;

    print_log("[NAT_TCP] tab %d new remote TCP conn", entry);

    /* Set up the various callback functions */
    tcp_recv(pcb, nat_tcp_recv);
    tcp_err(pcb, nat_tcp_err);
//    tcp_poll(pcb, nat_tcp_poll, HTTPD_POLL_INTERVAL);
    tcp_sent(pcb, nat_tcp_sent);
    nat_stat.tcp_new++;
    write(nat_task_pipe[1], "1",1);// notify for new connection
    write(nat_rx_task_pipe[1], "1",1);// notify for new connection

    return ERR_OK;
}

void eth_nat_init() {
    int ret=pipe2(&nat_task_pipe, O_NONBLOCK);
    ret=pipe2(&nat_rx_task_pipe, O_NONBLOCK);
    if (ret) return;

    fcntl(nat_task_pipe[0], F_SETFL, O_NONBLOCK);
    fcntl(nat_rx_task_pipe[0], F_SETFL, O_NONBLOCK);
    fcntl(nat_task_pipe[1], F_SETFL, O_NONBLOCK);
    fcntl(nat_rx_task_pipe[1], F_SETFL, O_NONBLOCK);

//    TNotifyCreate(&tNotifyHandle);
    TaskCreate (NAT_Task, &tnatHandle);
    TaskCreate (NAT_RX_Task, &tnatRXHandle);

    pthread_t tstats;
    TaskCreate (calc_stats, &tstats);


    nat_tcp=tcp_new_ip_type(IPADDR_TYPE_V4);
    if (nat_tcp==NULL) return;
    tcp_bind(nat_tcp, IP_ANY_TYPE, NAT_DIVERT_PORT);
    err_t err;
    nat_tcp=tcp_listen_with_backlog_and_err(nat_tcp, 10, &err);
    if (nat_tcp==NULL) { print_log("[NAT_TCP] === NAT tcp listen failed (err %d)", err); return; }
    tcp_accept(nat_tcp, nat_accept_fn);
//  print_log("[NAT_TCP] === NAT INIT");

}
