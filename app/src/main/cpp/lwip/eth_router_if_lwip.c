// LWIP interface for router

#include <string.h>

#include "lwip/lwip.h"
#include "lwip/opt.h"
#include "lwip/timeouts.h"
#include "netif/ethernet.h"
#include "netif/etharp.h"
#include "lwip/tcpip.h"

#include "lwip/pbuf.h"

#include "eth_mem.h"
#include "eth_router_cfg.h"
#include "eth_router.h"
#include "lwip/eth_router_if_lwip.h"

static struct ETH_QUEUEPOOL_CTRL* if_pool=&xeth_queuepool_ctrl;

struct netif* lwip_netif;
static pthread_t xLWIP_input_task;
static pthread_t xLWIP_tmr_task;
struct NHandle_t TTskHandle;

struct ETH_IF_LWIP_STATS eth_if_lwip;

static char if_name[]="LWIP";

struct ETH_PBUF* block_to_send;


//#pragma optimize=none
static err_t lwip_output_to_bridge(struct netif *netif, struct pbuf *p) {
  if (p->tot_len>ETH_MEM_BUF_SIZE) {
    eth_if_lwip.in_too_big++;
    return ERR_OK;
    }
  struct ETH_PBUF* block=eth_pbuf_alloc(&xeth_pbuf_ctrl, NULL);
  if (block==NULL) {
    eth_if_lwip.in_alloc_errs++;
    return ERR_OK;
    }
  int ptr=0;
  struct pbuf *frag;
  for (frag=p; frag!=NULL; frag=frag->next) {// copy datagram payload from LWIP to ETH_MEM pbuf
    if (frag->len+ptr<=ETH_MEM_BUF_SIZE) memcpy((uint8_t*)block->pbuf.payload+ptr, frag->payload, frag->len);
    else {
      eth_pbuf_free_request(block);
      eth_if_lwip.in_too_big++;
      return ERR_OK;
      }
    ptr+=frag->len;
    }
//  print_log("dgram from LWIP, size=%d",ptr);
  block->pbuf.len=ptr;
  block->pbuf.tot_len=ptr;
  block->pbuf.if_idx=LWIP_NETIF_ID;
  for (int i=0; i<6; i++) ((uint8_t*)(block->pbuf.payload))[i+6]=netif->hwaddr[i];

  eth_add_packet_to_router_queue(block);
  return ERR_OK;
  }


static void to_lwip_QueueTask(void* arg) {
  print_log("********** LWIP task started");
  while (1) {
    int res=TNotifyWait( &TTskHandle );
    if (res & 2) {
      LOCK_LWIP_CORE();
      sys_check_timeouts();
      UNLOCK_LWIP_CORE();
      }
    if (res & 1) do {
      if (block_to_send->pbuf.ref==0) while(1);

      int in=0;
      int err=1;
      LOCK_LWIP_CORE();
      if (lwip_netif->input) err=lwip_netif->input(&block_to_send->pbuf, lwip_netif);
      UNLOCK_LWIP_CORE();
//      if (err == ERR_OK) in=1;
//  print_log("dgram to LWIP, size=%d",block_to_send->pbuf.len);
      if (err != ERR_OK) eth_pbuf_free_request(block_to_send);
//      if (in==0) {
//        eth_if_lwip.lwip_out_errs++; eth_if_lwip.lwip_out_err=err;
//        LOCK_LWIP_CORE();
//        if (lwip_netif->input) err=lwip_netif->input(&block_to_send->pbuf, lwip_netif);
//        UNLOCK_LWIP_CORE();
//        if (err) eth_pbuf_free_request(block_to_send);
//        }
        block_to_send=NULL;
        struct ETH_PBUF* tb=eth_get_packet(&if_pool->eth_netif_array[LWIP_NETIF_ID]);
        if (tb) block_to_send=tb;
      } while (block_to_send);

    }
  }

static void to_lwip_TmrTask(void* arg) {
  while(1) {
    TNotifyAdd( &TTskHandle, 2);
    usleep(1000*100);
    }
  }

static void lwip_callback_from_bridge(struct ETH_PBUF* block, struct ETH_NETIF* enetif) {
  block_to_send=block;
  TNotifyAdd( &TTskHandle, 1);
  }


err_t eth_lwip_if_init(struct netif *netif) {
  LWIP_ASSERT("netif != NULL", (netif != NULL));
  lwip_netif=netif;

  netif->name[0] = 'L';
  netif->name[1] = 'W';

  print_log("********** LWIP interface init");

  netif->output = etharp_output;

  netif->linkoutput = lwip_output_to_bridge;//low_level_output;

  for (int i=0; i<6; i++) netif->hwaddr[i] = rand();
  netif->hwaddr[0]=0x00;
  netif->hwaddr[1]=0x80;
  netif->hwaddr_len = ETH_HWADDR_LEN;
  netif->mtu = 1500;//ETH_MAX_PAYLOAD;

  netif->flags |= NETIF_FLAG_BROADCAST | NETIF_FLAG_ETHARP;

  if (xLWIP_input_task==NULL) {
    if_pool->eth_netif_array[LWIP_NETIF_ID].callback_fn=lwip_callback_from_bridge;
    TaskCreate (to_lwip_QueueTask, &xLWIP_input_task);
    TaskCreate (to_lwip_TmrTask, &xLWIP_tmr_task);
    TNotifyCreate(&TTskHandle);
    while ( eth_get_packet(&if_pool->eth_netif_array[LWIP_NETIF_ID]) );
    }

  if_pool->eth_netif_array[LWIP_NETIF_ID].q_output_len_max=LWIP_NETIF_QLEN;
  if_pool->eth_netif_array[LWIP_NETIF_ID].if_name=if_name;

  netif_set_up(netif);
  netif_set_link_up(netif);
  if_pool->eth_netif_array[LWIP_NETIF_ID].if_up=1;
  return ERR_OK;
  }
