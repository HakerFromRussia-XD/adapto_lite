#define NAT_TAB_SIZE 16

#define SNDQ_MAXLEN 100

struct NAT_TAB {
  uint32_t src_ip;// порядок байт в сетевом формате
  uint32_t dst_ip;// порядок байт в сетевом формате
  uint16_t src_port;// порядок байт в сетевом формате
  uint16_t dst_port;// порядок байт в сетевом формате
  uint16_t proto;
  uint32_t time;

  struct pbuf* p[SNDQ_MAXLEN];// очередь приема с lwip
  uint8_t txbuf[1500];// на передачу в lwip
  uint16_t txbuf_len;
  struct tcp_pcb *pcb;// null - free
  uint32_t index_begin;
  uint32_t index_end;
  uint32_t status;// 0-free, 1-active (for TCP-based entries)
  int socket;
  uint32_t offs;
  uint32_t errors;
  uint32_t rx_ovf;
  uint32_t shutdown_req;// 1 - waiting for shutdown sockets
  };

struct NAT_STAT {
  uint32_t pbuf_broken;
  uint32_t tcp_new;
  uint32_t tcp_close;
  uint32_t pbuf_used;
  uint32_t lw_conn_used;
  uint32_t so_conn_used;
  };
extern struct NAT_STAT nat_stat;


int forward_nat(struct ETH_QUEUE* block);
int backward_nat(struct ETH_QUEUE* block);
void eth_nat_init();
