#define LWIP_TASK_STACK_SIZE 1024// x4 bytes
err_t eth_lwip_if_init(struct netif *netif);
extern struct netif* lwip_netif;

struct ETH_IF_LWIP_STATS {
  uint32_t in_frags;
  uint32_t in_too_big;
  uint32_t in_alloc_errs;
  uint32_t lwip_out_errs;
  err_enum_t lwip_out_err;
  };

extern struct ETH_IF_LWIP_STATS eth_if_lwip;
