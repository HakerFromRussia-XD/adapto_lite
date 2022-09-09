#include <stdint.h>
#include <string.h>
#include <pthread.h>
#include <semaphore.h>

#include "eth_mem.h"
#include "eth_router.h"
#include "eth_router_cfg.h"
#include "eth_nat.h"

struct ETH_ROUTER_STATS eth_router_stats;

struct ETH_QUEUEPOOL_CTRL xeth_queuepool_ctrl;
struct ETH_QUEUE xeth_queuepool[ETH_MEM_BUFS_CNT] ETH_MEM_ALIGN;

struct ETH_PBUF_CTRL xeth_pbuf_ctrl;
struct ETH_PBUF eth_pbuf[ETH_PBUFS_CNT];
uint8_t eth_pbuf_mem[ETH_PBUFS_CNT*ETH_MEM_BUF_SIZE];// граница элементов кратна 32 байтам. иначе будут проблемки с кешем.


struct ETH_NETIF eth_netif_array[ETH_NETIFS];

void eth_router_init() {
  eth_router_add_bridge(&xeth_queuepool_ctrl, xeth_queuepool, ETH_MEM_BUFS_CNT, &xeth_pbuf_ctrl, eth_pbuf, eth_pbuf_mem, ETH_PBUFS_CNT, eth_netif_array, ETH_NETIFS);
  eth_router_start_task(&xeth_queuepool_ctrl);
  }


#define ETHTYPE_IPV4 0x0800
#define ETHTYPE_IPV6 0x86DD
#define ETHTYPE_ARP 0x0806

#define ETHTYPE_ADP 0x8777// braodcast топологии
#define ETHTYPE_DBG 0x8778// unicast diagram
#define ETHTYPE_CONTROLS 0x8779// controls, terminal, gui controls


void sprint_pkt(char* pkt, int pkt_len) {
  char textbuf[1000];
  char textptr=0;

  textptr+=sprintf(textbuf+textptr,"DMAC=%02X:%02X:%02X:%02X:%02X:%02X ",pkt[0],pkt[1],pkt[2],pkt[3],pkt[4],pkt[5]);
  textptr+=sprintf(textbuf+textptr,"SMAC=%02X:%02X:%02X:%02X:%02X:%02X ",pkt[6],pkt[7],pkt[8],pkt[9],pkt[10],pkt[11]);
  uint16_t ethertype=(pkt[12]<<8)+pkt[13];
  textptr+=sprintf(textbuf+textptr,"ET=%04X ",ethertype);

  if (ethertype == ETHTYPE_ARP) {
    textptr+=sprintf(textbuf+textptr,"[ARP] ");
    }
  if (ethertype == ETHTYPE_IPV4) {
    textptr+=sprintf(textbuf+textptr,"[IP4] ");
    }
  if (ethertype == ETHTYPE_ADP) {
    textptr+=sprintf(textbuf+textptr,"[ADP] ");
    }
  if (ethertype == ETHTYPE_DBG) {
    textptr+=sprintf(textbuf+textptr,"[DBG] ");
    }
  if (ethertype == ETHTYPE_CONTROLS) {
    textptr+=sprintf(textbuf+textptr,"[CTR] ");
    }

  if (ethertype == ETHTYPE_ADP || ethertype == ETHTYPE_DBG || ethertype == ETHTYPE_CONTROLS) {

    }

  textptr+=sprintf(textbuf+textptr,"LEN=%d ",pkt_len);

  print_log("%s",textbuf);
  
  }

int prev_num;
void eth_route_block(struct ETH_QUEUE* block) {


  if (block->pbuf->pbuf.ref==0) EM_ASSERT(20);

  int pbuf_num=(block->pbuf-eth_pbuf);

  uint32_t src_netif=block->pbuf->pbuf.if_idx;


//  sprint_pkt(block->pbuf->pbuf.payload, block->pbuf->pbuf.len);

/*if (src_netif == LWIP_NETIF_ID) {
  print_log("Route pbuf from lwip");
  sprint_pkt(block->pbuf->pbuf.payload, block->pbuf->pbuf.len);
  }*/

  if (src_netif>=ETH_NETIFS) EM_ASSERT(21);// assert: invalid netif
  if (src_netif==0) EM_ASSERT(22);// assert: invalid netif

  if (src_netif==TEST_NETIF_ID) eth_router_stats.in_test++;
  if (src_netif==BLE_NETIF_ID) eth_router_stats.in_ble++;

  if (eth_netif_array[src_netif].if_up==0) {// src interface is down: drop packet & exit
    block->pool->eth_netif_array[src_netif].dgrams_lost++;
    eth_pbuf_free_request(block->pbuf);
    eth_queue_entry_free(block);
    print_log("interface (%d) is not up, dropping dgram ",src_netif);
    return;
    }

//  block->pool->eth_netif_array[src_netif].dgrams_lost++;

  uint32_t dst_netif=src_netif;
  if (src_netif == BLE_NETIF_ID) {
    forward_nat(block);
    dst_netif=LWIP_NETIF_ID;
    }
  if (src_netif == LWIP_NETIF_ID) {
    backward_nat(block);
    dst_netif=BLE_NETIF_ID;
    }

  if (dst_netif>=ETH_NETIFS) EM_ASSERT(23);// assert: invalid netif
  if (dst_netif==0) {
    eth_pbuf_free_request(block->pbuf);
    eth_queue_entry_free(block);
    return;
    }
  eth_add_packet_to_output_queue(block, &block->pool->eth_netif_array[dst_netif]);
  }
