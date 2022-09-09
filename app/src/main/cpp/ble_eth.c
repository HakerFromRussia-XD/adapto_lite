#include <stdint.h>
#include <string.h>
#include <pthread.h>
#include "xlib.h"
#include "ble_eth.h"
#include "eth_mem.h"
#include "eth_router.h"
#include "ble_tcp.h"

//#define BLE_FLOOD_TEST
// модуль разобрки-сборки кодограмм на фрагменты. Работает вместе с ble_tcp и с ble_link

/* Формат посылки:
Кодограмма разбиватеся на куски. К каждому куску в начале добавляется один байт.
Формат байта: байт0 биты 6,7 - тип посылки. Биты 0-5 будут перезаписаны при передаче.
  bit7:6 - тип кодограммы:
  00 (0x00) - промежуточный блок кодограммы
  01 (0x40) - первый блок кодограммы
  10 (0x80) - последний блок кодограммы
  11 (0xC0) - вся кодограмма в одном фрагменте
*/

struct BLE_ETH_STATS ble_eth_stats;

static struct ETH_PBUF* current_tx_block=NULL;
static uint8_t eth_rst_req_rx;
static uint8_t tx_rst=0;

void gatt_eth_reset() {
  eth_rst_req_rx=1;
  tx_rst=1;
  }

static struct ETH_PBUF* rx_pbuf=NULL;
static uint16_t rx_ptr;

static uint8_t rx_prev_seq;
static uint8_t rx_seq_err;

int process_dgram_asm(uint8_t* data, uint16_t len) {// BLE to ETH_router

  ble_eth_stats.rx_chunks++;
  if (len<2) { ble_eth_stats.rx_too_short_chunk++; return 0; }

//  if (eth_netif_array[BLE_NETIF_ID].input_queue_len>IN_BLE_NETIF_QLEN_THR) { ble_eth_stats.rx_qpause++; return -1; }// очередь заполнена, временно не принимаем новое (loopback test mode)
// если очередь полная, то приостановим поток

  if (eth_rst_req_rx) {
    eth_rst_req_rx=0;
    if (rx_pbuf) {
      eth_pbuf_free_request(rx_pbuf);
      rx_pbuf=NULL;
      }
    rx_ptr=0;
    }

  uint8_t dg_type=data[0]&0x0C0;
  uint8_t dg_seq=data[0]&0x03F;
  if (dg_type & 0x40) {// first block
    rx_ptr=0;
    rx_seq_err=0;
    rx_prev_seq=(dg_seq-1)&0x03F;
    if (rx_pbuf==NULL) rx_pbuf=eth_pbuf_alloc(&xeth_pbuf_ctrl, NULL);
    if (rx_pbuf==NULL) { ble_eth_stats.rx_pbuf_alloc_err++; return -1; }
    }
  if (rx_pbuf!=NULL) {
    rx_prev_seq=(rx_prev_seq+1)&0x3F;
    if (rx_prev_seq!=dg_seq || rx_seq_err) {
      ble_eth_stats.rx_seqerr++;
      eth_pbuf_free_request(rx_pbuf);
      rx_pbuf=NULL;
      return 0;
    }
    len--;
    if (rx_ptr+len<=ETH_MEM_BUF_SIZE) {
      memcpy((uint8_t*)rx_pbuf->pbuf.payload+rx_ptr, data+1, len);
      rx_ptr+=len;
      if (dg_type & 0x080) {// last block
        rx_pbuf->pbuf.len=rx_ptr;
        rx_pbuf->pbuf.tot_len=rx_ptr;
        rx_pbuf->pbuf.if_idx=BLE_NETIF_ID;
        if (rx_ptr>=14) { ble_eth_stats.rx_dgrams++; eth_add_packet_to_router_queue(rx_pbuf); }
        else { ble_eth_stats.rx_too_short_dgram++; eth_pbuf_free_request(rx_pbuf); }
        rx_pbuf=NULL;
        }
      }
    else {// invalid block
      ble_eth_stats.rx_oversize++;
      eth_pbuf_free_request(rx_pbuf);
      rx_pbuf=NULL;
      }
    }
  return 0;
  }



#define MAX_CHUNK_LEN 167//239
static uint16_t chunk_len;// for BLE - current chunk length
static uint16_t tx_ptr;// pointer for chunks

static int cbk_pending=0;
int get_next_chunk(uint8_t* chunk_for_tx) {// взять новый блок. Или NULL если блока нет (если блока нет - будет вызван коллбэк)

  if (tx_rst) {
    tx_rst=0;
    tx_ptr=0;
  }

#ifdef BLE_FLOOD_TEST
  ble_eth_stats.tx_chunks++;
  chunk_for_tx[0]=0xC0;
  return 240;// for test
#endif
  if (cbk_pending) return 0;
  if (current_tx_block==NULL) {
      cbk_pending=1;
      struct ETH_PBUF* tb=eth_get_packet(&eth_netif_array[BLE_NETIF_ID]);
      if (tb==NULL) {
        return 0;// waiting for callback
        } else { current_tx_block=tb; cbk_pending=0; }

      }

  uint8_t pkt_sent;// 0 - no data; 1 - send in progress, 2 - send finished
  int32_t len=current_tx_block->pbuf.len-tx_ptr;
  if (tx_ptr==0) chunk_for_tx[1]=0x40; else chunk_for_tx[1]=0;
  if (len>MAX_CHUNK_LEN) { len=MAX_CHUNK_LEN; pkt_sent=0; } else { chunk_for_tx[1]|=0x80; pkt_sent=1; }
  chunk_len=len+2;
  memcpy(&chunk_for_tx[2], (uint8_t*)(current_tx_block->pbuf.payload)+tx_ptr, len);
  tx_ptr+=len;

  if (pkt_sent) {// последний блок скопирован в chunk_for_tx. пакат можно освобождать
    ble_eth_stats.tx_dgrams++;
    eth_pbuf_free_request(current_tx_block);
    current_tx_block=NULL;
    tx_ptr=0;
    }

  ble_eth_stats.tx_chunks++;
  return chunk_len;
  }

void next_chunk_avail();

static void callback_from_bridge(struct ETH_PBUF* block, struct ETH_NETIF* enetif) {// появился пакет на передачу
  current_tx_block=block;
  cbk_pending=0;
  next_chunk_avail();
  }

void ble_callback_qlen(struct ETH_NETIF* netif) {
  if (netif->input_queue_len<IN_BLE_NETIF_QLEN_THR) {// обновилась очередь в роутере - позволяет забрать пакет
    notify_ble_out_queue();
    }
  }

_Noreturn void tcp_test_Task(void * arg) {
  while(1) {
    struct ETH_PBUF* block=eth_pbuf_alloc(&xeth_pbuf_ctrl, NULL);
    if (block) {
      memset(block->pbuf.payload, 0,1000);
      char* d=(char*)block->pbuf.payload;
      d[0]=1;
      d[1]=2;
      d[2]=3;
      d[3]=4;
      d[4]=5;
      d[5]=6;

      block->pbuf.len=230;
      block->pbuf.tot_len=230;
      block->pbuf.if_idx=LWIP_NETIF_ID;
      eth_add_packet_to_router_queue(block);
      }
    usleep(1000*500);
    }
  }
pthread_t ttHandle;

void eth_ble_if_init() {
  eth_netif_array[BLE_NETIF_ID].callback_fn=callback_from_bridge;// инициализируем callback (пакет готов)
  eth_netif_array[BLE_NETIF_ID].q_output_len_max=BLE_NETIF_QLEN;
  eth_netif_array[BLE_NETIF_ID].q_update_cbk=ble_callback_qlen;
  eth_netif_array[BLE_NETIF_ID].if_up=1;
//  TaskCreate (tcp_test_Task, &ttHandle);

  eth_netif_array[TEST_NETIF_ID].q_output_len_max=BLE_NETIF_QLEN;
  eth_netif_array[TEST_NETIF_ID].if_up=1;
  }
