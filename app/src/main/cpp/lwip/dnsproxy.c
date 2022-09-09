#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>
#include "lwip/opt.h"
#include "lwip/udp.h"
#include "lwip/mem.h"
#include "dnsproxy.h"
#include "eth_router.h"
#include "eth_queue.h"

static u8_t mdns_payload[DNS_MSG_SIZE];

static struct udp_pcb *pdns_pcb = NULL;

struct ETH_Q dns_q;

static struct NHandle_t tNotifyHandle;
static pthread_t tHandle;

#define MDNS_MAX_NAME_LENGTH      (256)

struct __attribute__((__packed__)) mdns_hdr {
	u16_t id;
	u8_t  flags1;
	u8_t  flags2;
	u16_t numquestions;
	u16_t numanswers;
	u16_t numauthrr;
	u16_t numextrarr;
};

#define SIZEOF_DNS_HDR 12

struct __attribute__((__packed__)) mdns_query {
	/* MDNS query record starts with either a domain name or a pointer
	 to a name already present somewhere in the packet. */
    u16_t type;
	u16_t class;
};

#define SIZEOF_DNS_QUERY 4

struct __attribute__((__packed__)) mdns_answer {
	/* MDNS answer record starts with either a domain name or a pointer
	 to a name already present somewhere in the packet. */
    u16_t type;
	u16_t class;
	u32_t ttl;
	u16_t len;
};
#define SIZEOF_DNS_ANSWER 10

struct DNS_RECORD {
    uint16_t query_type;
    uint16_t query_class;
    uint8_t name[512];
    uint32_t ip;
    uint16_t port;
    uint16_t id;
};

static int query_decode(uint8_t* query, int len, struct DNS_RECORD* out) {
  int pos=0;
  int outpos=0;
  do {
    if (pos>=len) return -1;
    uint8_t nlen=query[pos++];
    if (nlen==0) break;
    if (outpos+nlen>=sizeof(out->name)-1) return -1;
    if (pos+nlen>len) return -1;
    for (int i=0; i<nlen; i++) out->name[outpos++]=query[pos++];
    out->name[outpos++]='.';
    } while(1);
  if (pos+4>len) return -1;
  if (outpos>0) outpos--;
  out->name[outpos]=0;
  out->query_type=(query[pos]<<8)+query[pos+1];
  out->query_class=(query[pos+2]<<8)+query[pos+3];
  return pos+4;
  }

static void pdns_recv(void *arg, struct udp_pcb *pcb, struct pbuf *p, struct ip_addr *iaddr, u16_t port) {
	u8_t i;
	struct mdns_hdr *hdr;
	u8_t nquestions;
	LWIP_UNUSED_ARG(arg);
	LWIP_UNUSED_ARG(pcb);
	struct mdns_info *info = (struct mdns_info *)arg;
	/* is the dns message too big ? */
	if (p->tot_len > DNS_MSG_SIZE) {
		LWIP_DEBUGF(DNS_DEBUG, ("dns_recv: pbuf too big\n"));
		/* free pbuf and return */
		goto memerr1;
	}

	/* is the dns message big enough ? */
	if (p->tot_len < (SIZEOF_DNS_HDR + SIZEOF_DNS_QUERY + SIZEOF_DNS_ANSWER)) {
		LWIP_DEBUGF(DNS_DEBUG, ("dns_recv: pbuf too small\n"));
		/* free pbuf and return */
		goto memerr1;
	}
	/* copy dns payload inside static buffer for processing */
	if (pbuf_copy_partial(p, mdns_payload, p->tot_len, 0) != p->tot_len) goto memerr1;

	hdr = (struct mdns_hdr*) mdns_payload;
    uint8_t* data=(uint8_t*) mdns_payload + SIZEOF_DNS_HDR;

    struct ETH_PBUF* p_req=eth_pbuf_alloc(&xeth_pbuf_ctrl, NULL);
    if (p_req==NULL) goto memerr1;

    print_log("DNS request: id=%04X fg1=%02X fg2=%02X nq=%d na=%d", htons(hdr->id), hdr->flags1, hdr->flags2, htons(hdr->numquestions), htons(hdr->numanswers));
    int num_q=htons(hdr->numquestions);
    int num_a=htons(hdr->numanswers);
    if (num_a!=0) goto memerr2;
    if (num_q!=1) goto memerr2;
    int remain_len=p->tot_len-SIZEOF_DNS_HDR;
    struct DNS_RECORD* rec=(struct DNS_RECORD*)p_req->pbuf.payload;
    int len=query_decode(data, remain_len, rec);
    if (len<0) goto memerr2;
    rec->id=htons(hdr->id);
    rec->ip=*(uint32_t*)iaddr;
    rec->port=port;
    print_log("DNS request: <%s> from ip=%d.%d.%d.%d port=%d", rec->name, (rec->ip)&0xff, (rec->ip>>8)&0xff, (rec->ip>>16)&0xff, (rec->ip>>24)&0xff, rec->port);
    add_to_custom_queue(&p_req->pbuf, &dns_q);
    TNotifyAdd( &tNotifyHandle, 1);

    goto memerr1;
memerr2:
    eth_pbuf_free_request(p_req);
memerr1:
	pbuf_free(p);
	return;
    }
// a a a . b b b . c c c 0
// 3 a a a 3 b b b 3 c c c 0
// 0 1 2 3 4 5 6 7 8 9 B C D
    int dns_encode_name(uint8_t* dst, uint8_t* str) {
    int pos=0;
    int cnt=0;
    do {
        if (str[pos]=='.') { dst[pos-cnt]=cnt; cnt=0; pos++; continue;}
        if (str[pos]==0) { dst[pos-cnt]=cnt; dst[pos+1]=0; pos+=2; break; }
        dst[pos+1]=str[pos];
        pos++; cnt++;
    } while(1);
    return pos;
    }

_Noreturn void dnsproxy_thread(void* arg) {

  struct pbuf* p;
  while (1) {
    uint32_t res=TNotifyWait( &tNotifyHandle );
    do {
     p=get_from_custom_queue(&dns_q);
     if (p==NULL) break;

     struct DNS_RECORD* rec=(struct DNS_RECORD*)p->payload;

     int status;
     struct addrinfo hints;
     struct addrinfo *servinfo;  // указатель на результаты
     memset(&hints, 0, sizeof hints); // убедимся, что структура пуста
     hints.ai_family = PF_INET;//AF_UNSPEC - AF_INET;     // неважно, IPv4 или IPv6
     hints.ai_socktype = SOCK_STREAM; // TCP stream-sockets
     hints.ai_flags = AI_PASSIVE;     // заполните мой IP-адрес за меняif ((status = getaddrinfo(NULL, «3490», &hints, &servinfo)) != 0) {
     status = getaddrinfo(rec->name, NULL, &hints, &servinfo);// servinfo теперь —
     if (status==0 && servinfo->ai_family==AF_INET) {
         uint8_t* ip=(uint8_t*)&((struct sockaddr_in *)servinfo->ai_addr)->sin_addr;
         print_log("DNS query service: <%s> IP=%d.%d.%d.%d", rec->name, ip[0], ip[1], ip[2], ip[3]);

         struct pbuf* p1 = pbuf_alloc(PBUF_TRANSPORT, 1400, PBUF_RAM);
         if (p1 != NULL) {
             int pos=0;
             struct mdns_hdr* hdr=p1->payload;
             hdr->id=htons(rec->id);
             hdr->flags1=0x81;
             hdr->flags2=0x80;
             //        FLAGS1    FLAGS2      Flags: 0x8180 Standard query response, No error
             //        1... .... .... .... = Response: Message is a response
             //        .000 0... .... .... = Opcode: Standard query (0)
             //        .... .0.. .... .... = Authoritative: Server is not an authority for domain
             //        .... ..0. .... .... = Truncated: Message is not truncated
             //        .... ...1 .... .... = Recursion desired: Do query recursively
             //        .... .... 1... .... = Recursion available: Server can do recursive queries
             //        .... .... .0.. .... = Z: reserved (0)
             //        .... .... ..0. .... = Answer authenticated: Answer/authority portion was not authenticated by the server
             //        .... .... ...0 .... = Non-authenticated data: Unacceptable
             //        .... .... .... 0000 = Reply code: No error (0)

             hdr->numquestions=htons(1);
             hdr->numanswers=htons(1);
             hdr->numauthrr=htons(0);
             hdr->numextrarr=htons(0);
             uint8_t dpos=sizeof(struct mdns_hdr);
             uint8_t* rsp=(uint8_t*)p1->payload;
             int ret=dns_encode_name(rsp+dpos, rec->name);
             if (ret>0) {
                 dpos += ret;
                 rsp[dpos++]=0;// TYPE
                 rsp[dpos++]=1;
                 rsp[dpos++]=0;// CLASS
                 rsp[dpos++]=1;
                 rsp[dpos++]=0xc0;// answer: name
                 rsp[dpos++]=0x0c;
                 rsp[dpos++]=0;// type
                 rsp[dpos++]=1;
                 rsp[dpos++]=0;// class
                 rsp[dpos++]=1;
                 rsp[dpos++]=0;// TTL
                 rsp[dpos++]=0;// TTL
                 rsp[dpos++]=0x27;// TTL
                 rsp[dpos++]=0x10;// TTL
                 rsp[dpos++]=0;// data length
                 rsp[dpos++]=4;// data length
                 rsp[dpos++]=ip[0];
                 rsp[dpos++]=ip[1];
                 rsp[dpos++]=ip[2];
                 rsp[dpos++]=ip[3];
                 p1->len=dpos;
                 p1->tot_len=dpos;
                 char ans[3000]; int pt=0;
                 for (int i=0; i<dpos; i++) {pt+=sprintf(ans+pt,"%02X ",rsp[i]);}
                 print_log("DNS answer: <%s>", ans);
                 udp_sendto(pdns_pcb, p1, &rec->ip, rec->port);
             }
             pbuf_free(p1);
         }

         } else print_log("DNS query err (status=%d)", status);
     pbuf_free(p);
     } while(1);

    }

  }


void dnsproxy_init() {
  dns_q.q_len_max=5;
  int ret;
  pdns_pcb = udp_new();
  if (pdns_pcb) {
	ret=udp_bind(pdns_pcb, IP_ADDR_ANY, 53);
    if (ret) return;
    udp_recv(pdns_pcb, pdns_recv, NULL);
    }

  TaskCreate (dnsproxy_thread, &tHandle);
  TNotifyCreate(&tNotifyHandle);

  }
