#include <jni.h>
#include <pthread.h>
#include <semaphore.h>
#include <string.h>
#include "ble_tcp.h"
#include "xlib.h"
#include "bt_drv.h"
#include "eth_mem.h"
#include "eth_router.h"
#include "ble_eth.h"
// модуль обеспечивающий ретрансмиссии, буферизацию, контроль потока для GATT посылок.
#define ACK_MIN_INTERVAL 25// mS. на эту величину может задерживаться отправка ACK
//#define print_pkt_body

static uint32_t last_ack_time;
static uint8_t ack_sent_value;
static uint8_t snd_blk_len;
static uint8_t snd_blk[MAX_CHUNK_LEN+2];

static struct itimerspec trigger;
static timer_t timerid;
static uint8_t ack_notify_request;

#define TMR_CHECK_TXBUF_PERIOD 100

//static TimerHandle_t xTcpTimer;
static struct NHandle_t tNotifyHandle;
pthread_t tHandle;
pthread_t tHandle1;

sem_t tcp_tx_sem;
sem_t tcp_rx_sem;

#define TX_LOCK sem_wait(&tcp_tx_sem)
#define TX_UNLOCK sem_post(&tcp_tx_sem)
#define RX_LOCK sem_wait(&tcp_rx_sem)
#define RX_UNLOCK sem_post(&tcp_rx_sem)

#define taskNOTIFY_ACK_UPDATE 1// очередь передачи
#define taskNOTIFY_NEXT_CHUNK_AVAIL 2
#define taskNOTIFY_NEXT_CHUNK_FOR_TX_AVAIL 4
#define taskNOTIFY_BLE_TX_COMPLETE 8
#define taskNOTIFY_nACK_UPDATE 16// приемная очередь
#define taskNOTIFY_TMR_IDLE 64
#define taskNOTIFY_PUSH_RECVD 256

struct BLE_TCP_STATS ble_tcp_stats;
struct BLE_TCP ble_tcp;


uint8_t tx_index_blk;// ячейка ожидающая передачу
uint8_t tx_push_ptr_blk;// ячейка ожидающая добавление элемента
static uint32_t tx_last_time;// ячейка ожидающая добавление элемента


uint8_t rx_nack_blk;// циклический номер блока, ожидающего приём

void gatt_conn_reset() {
  TX_LOCK;
  RX_LOCK;
  ble_tcp.ble_tx_busy=0;
  ble_tcp_stats.rst_cnt++;
  rx_nack_blk=1;
  tx_index_blk=0;
  tx_push_ptr_blk=0;
  snd_blk_len=0;
  gatt_eth_reset();
  TNotifyAdd( &tNotifyHandle, taskNOTIFY_BLE_TX_COMPLETE);
  RX_UNLOCK;
  TX_UNLOCK;
  }

void notify_ble_out_queue() {// callback: обновилась очередь ETH / ble_if
  TNotifyAdd(&tNotifyHandle, taskNOTIFY_PUSH_RECVD);
  }

void process_gatt_data(uint8_t* data, uint16_t len) {
  ble_tcp_stats.rx_raw++;
  if (len<1) { ble_tcp_stats.rx_empty_dg++; return; }
  ble_tcp_stats.rx_bytes+=len;
#ifdef print_pkt_body
  if (len==1) print_log("Recv %d bytes [%02x]", len, data[0]);
  else print_log("Recv %d bytes [%02x %02x]", len, data[0], data[1]);
#endif
  TX_LOCK;
  rx_nack_blk=data[0]&0x3F;
  TNotifyAdd( &tNotifyHandle, taskNOTIFY_nACK_UPDATE );
  TX_UNLOCK;

  if (len>1) {
    RX_LOCK;
    process_dgram_asm(data+1, len-1);
    RX_UNLOCK;
  }
}


void next_chunk_avail() {
  TNotifyAdd( &tNotifyHandle, taskNOTIFY_NEXT_CHUNK_AVAIL );
  }

void ble_tx_complete(int status) {
  ble_tcp.ble_tx_busy=0;
  TNotifyAdd( &tNotifyHandle, taskNOTIFY_BLE_TX_COMPLETE );// ззавершена передача. можно затолкать в буфер передачи еще
  }

static int ble_gatt_trysend(uint8_t * data, uint16_t len) {
  if (ble_connection_status!=2) return -3;// not ready for send
  if (ble_tcp.ble_tx_busy) return -1;
  ble_tcp.ble_tx_busy=1;
  int ret=bt_send(data, len);// status: 1 - all ok, 0 - error
  if (ret!=1) { ble_tcp.ble_tx_busy=0; return -2; }
  ble_tcp_stats.tx_raw++;

#ifdef print_pkt_body
  if (len==1) print_log("Send %d bytes [%02x]", len, data[0]);
  else print_log("Send %d bytes [%02x %02x]", len, data[0], data[1]);
#endif

  return 0;
  }

// байт 1 (или 1+2) - rx ack / sack
_Noreturn void BLE_TCP_Task(void * arg) {

  send_thread_register();// зарегистрировать в java VM этот поток для возможности совершать вызовы

  while (1) {
    uint32_t res=TNotifyWait( &tNotifyHandle );

// попробуем передать что накопилось
    if ((res & (taskNOTIFY_BLE_TX_COMPLETE | taskNOTIFY_NEXT_CHUNK_AVAIL | taskNOTIFY_nACK_UPDATE | taskNOTIFY_TMR_IDLE)) && ble_tcp.ble_tx_busy==0) {
dbg[0]++;
      TX_LOCK;

      int idx=tx_index_blk;
      int len=snd_blk_len;
      if (len==0) {
        len=get_next_chunk(&snd_blk[0]);
        snd_blk_len=len;
      }

      if (len || (ack_notify_request && ack_sent_value!=rx_nack_blk )) {// шлем если есть что слать, либо пора отправить обновление по ACK
        uint32_t ack=rx_nack_blk;

        snd_blk[0]=ack;
        if (len==0) len=1;
        else snd_blk[1]=(idx&0x3F)|(snd_blk[1] & 0xC0);// сохраняем биты признака начала-конца кодограммы для разобрщика кодограмм
// 167 = 5 (800) *9=1503 (169 bytes max incl. headers)
        int ret=ble_gatt_trysend(&snd_blk[0], len);// ack + dgram
        if (ret==0) {
          snd_blk_len=0;
          ble_tcp_stats.tx_bytes+=len;
          ble_tcp_stats.tx_blocks++;
          ack_sent_value=ack;
          ack_notify_request=0;
          timer_settime(timerid, 0, &trigger, NULL);
          tx_index_blk=(idx+1)&0x3F;
        }
      }

      TX_UNLOCK;
      }

    }
  }

static void tcp_tmr( void* arg ) {
  TX_LOCK;
  ack_notify_request=1;
  TX_UNLOCK;
  TNotifyAdd( &tNotifyHandle, taskNOTIFY_TMR_IDLE);// no activity. for timed-out retransmit
}

void ble_tcp_init() {
  sem_init(&tcp_tx_sem, 0, 1);
  sem_init(&tcp_rx_sem, 0, 1);

  tx_last_time=xTaskGetTickCount();
  TaskCreate (BLE_TCP_Task, &tHandle);
  TNotifyCreate(&tNotifyHandle);
  TNotifyAdd( &tNotifyHandle, taskNOTIFY_ACK_UPDATE);

  struct sigevent sev;
  memset(&sev, 0, sizeof(struct sigevent));
  sev.sigev_notify = SIGEV_THREAD;
  sev.sigev_notify_function = &tcp_tmr;
  memset(&trigger, 0, sizeof(struct itimerspec));
  timer_create(CLOCK_REALTIME, &sev, &timerid);
  trigger.it_value.tv_sec = ACK_MIN_INTERVAL/1000000;
  trigger.it_value.tv_nsec = ACK_MIN_INTERVAL*1000000;
  timer_settime(timerid, 0, &trigger, NULL);

  }
