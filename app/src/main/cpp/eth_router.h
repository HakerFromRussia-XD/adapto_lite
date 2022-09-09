#pragma once
#include "eth_mem.h"

struct __attribute__((__packed__)) MAC_ADDR {
  uint32_t mac32;
  uint16_t mac16;
  };

struct __attribute__((__packed__)) ETH_PKT_HDR {
  struct MAC_ADDR dst_mac;
  struct MAC_ADDR src_mac;
  uint16_t ethertype;
  uint8_t tos_hdrl;
  uint8_t dscp;
  uint16_t len;
  uint16_t id;
  uint16_t frag_offs;
  uint8_t ttl;
  uint8_t proto;
  uint16_t hdr_csum;
  uint32_t ip_src;
  uint32_t ip_dst;
  uint16_t port_src;// tcp/udp hdr
  uint16_t port_dst;
  };

struct MAC_LOOKUP_TAB {
  struct MAC_ADDR mac;
  uint16_t time_upd;
  uint8_t if_idx;
  };

struct ETH_ROUTER_STATS {
  uint32_t in_test;
  uint32_t in_ble;
  uint32_t out_if_not_found;
  uint32_t test_err;

  };
extern struct ETH_ROUTER_STATS eth_router_stats;

void eth_route_block(struct ETH_QUEUE* block);
uint32_t mac_search(struct MAC_ADDR mac);

// проинициализировать мост по умолчанию (запускает eth_router_add_bridge с дефолтным пулом)
void eth_router_init();
void notify_eth_ble_send_queue_update();
void notify_eth_uart_send_queue_update();

//extern struct ETH_QUEUE xeth_queuepool[ETH_MEM_BUFS_CNT];
extern struct ETH_QUEUEPOOL_CTRL xeth_queuepool_ctrl;
extern struct ETH_QUEUE xeth_queuepool[ETH_MEM_BUFS_CNT];

extern struct ETH_PBUF_CTRL xeth_pbuf_ctrl;
extern struct ETH_PBUF eth_pbuf[ETH_PBUFS_CNT];
extern uint8_t eth_pbuf_mem[ETH_PBUFS_CNT*ETH_MEM_BUF_SIZE];

extern struct ETH_NETIF eth_netif_array[ETH_NETIFS];
