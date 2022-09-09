// ETH_MEM ver 1.0

#include "xlib.h"
#include <string.h>
#include <pthread.h>
#include <semaphore.h>

#include "eth_mem.h"
#include "eth_router.h"

sem_t eth_mem_sem;

// ============ task for queue management
struct ETH_MEM_STATS eth_mem_stats;

// private function prototypes
static void EthQueueTask(void *argument);
static void eth_custom_pbuf_free_request(struct pbuf* p);

#define taskNOTIFY_NEW_FREE_P_REQUEST 1// освобождение элемента очереди
#define taskNOTIFY_NEW_P_REQUEST 4// дефицит pbuf
#define taskNOTIFY_ROUTER_Q_UPDATE 16// толкнуть обработчик очередей (роутер)
// ref увеличивается только в главном пакете из цепочки.
inline static struct ETH_PBUF* do_eth_pbuf_alloc(struct ETH_PBUF_CTRL* eth_pbuf_ctrl, struct NOMEM_CALLBACK* cb) {

  struct ETH_PBUF* p=(struct ETH_PBUF*)eth_pbuf_ctrl->free_chain;
  if (p==NULL) {// no memory
    if (cb) {
      if (cb->wait_active) return NULL;
      eth_mem_stats.eth_pbuf_alloc_delayed++;
      if (eth_mem_stats.eth_pbuf_alloc_delayed==2) {}
      cb->next_p=NULL;
      struct NOMEM_CALLBACK* nopbuf_queue_prev=(struct NOMEM_CALLBACK*)eth_pbuf_ctrl->pool->nopbuf_q_end;
      if (nopbuf_queue_prev) nopbuf_queue_prev->next_p=cb; else eth_pbuf_ctrl->pool->nopbuf_q_begin=cb;
      eth_pbuf_ctrl->pool->nopbuf_q_end=cb;
      cb->wait_active=1;
      }
    else eth_mem_stats.eth_pbuf_alloc_fails++;
    return NULL;
    }
  eth_pbuf_ctrl->free_chain=(struct ETH_PBUF*)p->pbuf.next;
  eth_pbuf_ctrl->used++;
  if (eth_pbuf_ctrl->used>eth_pbuf_ctrl->used_max) eth_pbuf_ctrl->used_max=eth_pbuf_ctrl->used;
  p->pbuf.next=NULL;
  int pbuf_num=(p-&eth_pbuf[0]);
  return p;
  }

struct ETH_PBUF* eth_pbuf_alloc(struct ETH_PBUF_CTRL* eth_pbuf_ctrl, struct NOMEM_CALLBACK* cb) {
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  struct ETH_PBUF* p=do_eth_pbuf_alloc(eth_pbuf_ctrl, cb);
  ETH_MEM_UNPROTECT();
  if (cb) {
    if (p==NULL) {
      TNotifyAdd(&eth_pbuf_ctrl->pool->tNotifyHandle, taskNOTIFY_NEW_P_REQUEST);
      return NULL;
      }
    p->pbuf.if_idx = cb->src_interface_id;
    }
  else p->pbuf.if_idx=0;

  p->pbuf.ref=1;
  p->pbuf.type_internal=0;// payload directly follows the struct pbuf
  p->pbuf.flags=PBUF_FLAG_IS_CUSTOM;// bufer have custom free function
  p->pbuf.payload=p->default_payload;

  return p;
  }


static struct ETH_QUEUEPOOL_CTRL* xpool;
// добавить мост. выделяем пул для пакетных буферов моста, память для интерфейсов
void eth_router_add_bridge(struct ETH_QUEUEPOOL_CTRL* pool, struct ETH_QUEUE* eth_q, uint32_t pool_mem_size_blocks,
                           struct ETH_PBUF_CTRL* eth_pbuf_ctrl, struct ETH_PBUF* eth_pbuf, uint8_t* eth_pbuf_mem, uint32_t eth_pbufs_cnt,
                           struct ETH_NETIF* netif_arr, uint32_t netif_count) {

// pbufs init

  eth_pbuf_ctrl->free_chain=eth_pbuf;
  eth_pbuf_ctrl->pool=pool;

  for (int i=0; i<eth_pbufs_cnt; i++) {// initialize all PBUFs
    eth_pbuf[i].default_payload=&eth_pbuf_mem[i*ETH_MEM_BUF_SIZE];// link PBUF with memory chunk (in separate space)
    eth_pbuf[i].pbuf.type_internal=0;// payload directly follows the struct pbuf
    eth_pbuf[i].pbuf.flags=PBUF_FLAG_IS_CUSTOM;// bufer have custom free function
    eth_pbuf[i].pbuf.ref=0;// reference count (1 - first allocate)
    eth_pbuf[i].pbuf.if_idx=0;// network interface index (0 = no index)
    eth_pbuf[i].src_if=0;// network interface index (0 = no index)
    eth_pbuf[i].custom_free_function=&eth_custom_pbuf_free_request;// pointer to custom free function
    eth_pbuf[i].ctrl=eth_pbuf_ctrl;// link for fast navigate to PBUF control structure
    }
  for (int i=0; i<eth_pbufs_cnt-1; i++) {// chain all PBUFs (allocation in future get PBUF from this chain)
    eth_pbuf[i].pbuf.next=(struct pbuf*)&eth_pbuf[i+1];
    }
  eth_pbuf[eth_pbufs_cnt-1].pbuf.next=NULL;


// pool init

  for (int i=0; i<pool_mem_size_blocks-1; i++) eth_q[i].next=&eth_q[i+1];// chain all free queue control structs for future allocation
  eth_q[pool_mem_size_blocks-1].next=NULL;// нужно только в случае переинициализации пула
  pool->used=0;// выделено блоков (статистика)
  pool->used_max=0;// пик выделения (статистика)
  pool->q_begin=eth_q;
  pool->eth_netif_array=netif_arr;
  pool->eth_netif_size=netif_count;

  for (int i=0; i<netif_count; i++) {
    netif_arr[i].q_output_end=NULL;
    netif_arr[i].q_output_begin=NULL;
    netif_arr[i].q_output_len=0;
    netif_arr[i].q_output_len_max=0;
    netif_arr[i].q_output_size=1;
    netif_arr[i].callback_fn=NULL;
    netif_arr[i].q_update_cbk=NULL;
    netif_arr[i].pool=pool;
    netif_arr[i].eth_netif_id=i;
    netif_arr[i].if_up=0;
    netif_arr[i].callback_pending=0;
    }


  for (int i=0; i<pool_mem_size_blocks; i++) {// проинициализируем заранее поля буферов один раз, чтобы не обновлять при каждой аллокации
    eth_q[i].state=st_free;
    eth_q[i].pool=pool;
    }
  xpool=pool;
  }


void eth_router_start_task(struct ETH_QUEUEPOOL_CTRL* pool) {
  sem_init(&eth_mem_sem, 0, 1);
  TNotifyCreate(&pool->tNotifyHandle);
  TaskCreate (EthQueueTask, &pool->pThr);
  }


static void eth_custom_pbuf_free_request(struct pbuf* p) {// callback from LWIP pbuf
  p->ref=1;// lwip уже декрементировал ref. Чтобы нам корректно освободить, нужно ref вернуть.
  eth_pbuf_free_request((struct ETH_PBUF*)p);
  }

void eth_pbuf_free_request(struct ETH_PBUF* xpbuf) {// добавлене в очередь на освобождение блока (или передачу другому владельцу)
  struct ETH_QUEUEPOOL_CTRL* pool=xpbuf->ctrl->pool;
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  if (xpbuf->pbuf.ref>1) {
    xpbuf->pbuf.ref--;
    ETH_MEM_UNPROTECT();
    return;
    }
  if (xpbuf->pbuf.ref==0) EM_ASSERT(1);// already free?

  int pbuf_num=(xpbuf-&eth_pbuf[0]);

  xpbuf->pbuf.next=NULL;//this pbuf must be last in chain
  if (pool->free_pbuf_q_end) {// pbuf is not empty
    if (pool->free_pbuf_q_end->pbuf.next) EM_ASSERT(2);// ASSERT: invalid pointer to last pbuf. last pbuf not marked as last in chain
    pool->free_pbuf_q_end->pbuf.next=&xpbuf->pbuf;// add to end of chain
    } else pool->free_pbuf_q_begin=xpbuf;
  pool->free_pbuf_q_end=xpbuf;
  ETH_MEM_UNPROTECT();
  TNotifyAdd(&pool->tNotifyHandle, taskNOTIFY_NEW_FREE_P_REQUEST);
  }

static void free_blk(struct ETH_QUEUEPOOL_CTRL* pool) {// освободим блоки из очереди на освобождение
      while (1) {
// взятие освобождаемого ETH_PBUF из очереди
        struct ETH_PBUF* xpbuf=(struct ETH_PBUF*)pool->free_pbuf_q_begin;// pbuf в начале очереди
        if (xpbuf==NULL) { /*ETH_MEM_UNPROTECT();*/ break; }
        if (xpbuf->pbuf.ref!=1) EM_ASSERT(3);// ASSERT: invalid allocation counter
        int src_if=xpbuf->src_if;
        xpbuf->src_if=0;
        ETH_MEM_DECL_PROTECT();
        ETH_MEM_PROTECT();
        pool->eth_netif_array[src_if].input_queue_len--;
        struct ETH_PBUF* n=(struct ETH_PBUF*)xpbuf->pbuf.next;// кто следующий в очереди
        pool->free_pbuf_q_begin=n;// продвигаем очередь
        if (n==NULL) pool->free_pbuf_q_end=NULL;// если очередь пуста
// действие с освобождаемым ETH_PBUF (освобождение или передача)
        if (pool->nopbuf_q_begin) {// pass memory to new owner directly
          struct NOMEM_CALLBACK* cb=(struct NOMEM_CALLBACK*)pool->nopbuf_q_begin;
          pool->nopbuf_q_begin=cb->next_p;
          if (pool->nopbuf_q_begin==NULL) pool->nopbuf_q_end=NULL;
          cb->wait_active=0;
          ETH_MEM_UNPROTECT();
          cb->next_p=NULL;// на всякий случай, в принципе это не нужно
          eth_mem_stats.eth_pbuf_alloc_delay_cbks++;
          xpbuf->pbuf.if_idx = cb->src_interface_id;
          cb->callback_fn(xpbuf, cb);// нужно выделить только pbuf
          }
        else {// free memory
          xpbuf->pbuf.next=(struct pbuf*)xpbuf->ctrl->free_chain;// освобождаемый блок добавляем в начало очереди
          xpbuf->ctrl->free_chain=xpbuf;
          xpbuf->pbuf.ref=0;// для порядка. в продакшене не требуется
          if (xpbuf->ctrl->used==0) EM_ASSERT(4);
          xpbuf->ctrl->used--;
          ETH_MEM_UNPROTECT();
          }
        if (pool->eth_netif_array[src_if].q_update_cbk) pool->eth_netif_array[src_if].q_update_cbk(&pool->eth_netif_array[src_if]);// вызовем callback, что очередь стала меньше
        }
  }

// Процесс, обслуживающий:
// - очередь запросов на освобождение памяти
// - очередь коллбэков на выделение памяти (тем кто в очереди на получение памяти при дефиците памяти)
// - очередь пакетов на роутинг
static void EthQueueTask(void* arg) {
  struct ETH_QUEUEPOOL_CTRL* pool=xpool;
  while (1) {
    uint32_t res=TNotifyWait(&pool->tNotifyHandle);

    if (res & taskNOTIFY_NEW_FREE_P_REQUEST) {// new pbuf_free requests
      free_blk(pool);
      }
    if (res & taskNOTIFY_ROUTER_Q_UPDATE) {// new dgram in queue
      while (1) {
        ETH_MEM_DECL_PROTECT();
        ETH_MEM_PROTECT();
        struct ETH_QUEUE* queue=(struct ETH_QUEUE*)pool->q_input_begin;
        if (queue==NULL) { ETH_MEM_UNPROTECT(); break; }
        struct ETH_QUEUE* n=queue->next;
        pool->q_input_begin=n;// забираем из очереди
        if (n==NULL) pool->q_input_end=NULL;
        queue->next=NULL;
        pool->q_input_used--;
        ETH_MEM_UNPROTECT();
        eth_route_block(queue);// и отдаём в роутинг
        if (pool->free_pbuf_q_begin) free_blk(pool);
        }
      }
    }

  }

// Выделить блок памяти.
inline static struct ETH_QUEUE* do_eth_queue_entry_alloc(struct ETH_QUEUEPOOL_CTRL* pool) {
  struct ETH_QUEUE* queue = (struct ETH_QUEUE*)pool->q_begin;// try to allocate queue
  if (queue==NULL) {
    pool->alloc_fails++;
    eth_mem_stats.qpool_alloc_fails++;
    return NULL;
    }
  pool->q_begin = queue->next;// update pointer to next free queue
  pool->used++;
  if (pool->used_max < pool->used) pool->used_max = pool->used;
  queue->state = st_allocated;
  return queue;// allocated memory
  }

static struct ETH_QUEUE* eth_queue_entry_alloc(struct ETH_QUEUEPOOL_CTRL* pool) {
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  struct ETH_QUEUE* q=do_eth_queue_entry_alloc(pool);
  ETH_MEM_UNPROTECT();
  return q;
  }

void eth_queue_entry_free(struct ETH_QUEUE* queue) {// освобождение элемента очереди
  struct ETH_QUEUEPOOL_CTRL* pool=queue->pool;
  if (queue->state==st_free) EM_ASSERT(5);// ASSERT: trying to free not used queue
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  queue->state=st_free;
  queue->next=(struct ETH_QUEUE*)pool->q_begin;// освобождаемый блок добавляем в начало очереди
  pool->q_begin=queue;
  pool->used--;
  ETH_MEM_UNPROTECT();
  }

#pragma optimize=none
void eth_add_packet_to_router_queue(struct ETH_PBUF* xpbuf) {
  uint32_t nif=xpbuf->pbuf.if_idx;
  if (xpbuf->pbuf.ref==0) EM_ASSERT(11);
  if (nif>=ETH_NETIFS) EM_ASSERT(6);//
  xpbuf->src_if=nif;
  if (xpbuf->pbuf.len<14) {// invalid datagram. 14 bytes (src mac+dst mac + ethertype) required
    eth_pbuf_free_request(xpbuf);
    ETH_MEM_DECL_PROTECT();
    ETH_MEM_PROTECT();
    xpbuf->ctrl->pool->eth_netif_array[nif].dgrams_lost++;
    ETH_MEM_UNPROTECT();
    return;
    }
  struct ETH_QUEUEPOOL_CTRL* pool=xpbuf->ctrl->pool;
  struct ETH_QUEUE* queue=eth_queue_entry_alloc(pool);
  if (queue==NULL) {
    eth_pbuf_free_request(xpbuf);
    ETH_MEM_DECL_PROTECT();
    ETH_MEM_PROTECT();
    pool->eth_netif_array[nif].dgrams_lost++;
    ETH_MEM_UNPROTECT();
    return;
    }
  queue->next = NULL;
  queue->state = st_in_queue;
  queue->pbuf = xpbuf;
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  pool->eth_netif_array[nif].input_queue_len++;
  pool->eth_netif_array[nif].dgrams_in++;
  if (pool->q_input_end!=NULL) pool->q_input_end->next=queue; else pool->q_input_begin=queue;
  pool->q_input_end=queue;
  pool->q_input_used++;
  if (pool->q_input_used_max<pool->q_input_used) pool->q_input_used_max=pool->q_input_used;

  ETH_MEM_UNPROTECT();
  TNotifyAdd(&pool->tNotifyHandle, taskNOTIFY_ROUTER_Q_UPDATE);
  }

struct ETH_PBUF* eth_get_packet(struct ETH_NETIF* enetif) {// запрос пакета из исходящей очереди драйвером для передачи во внешний мир
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  struct ETH_QUEUE* queue=(struct ETH_QUEUE*)enetif->q_output_begin;
  if (queue) {// packet ready
    struct ETH_QUEUE* next_queue=queue->next;
    enetif->q_output_begin=next_queue;
    if (next_queue == NULL) enetif->q_output_end = NULL;
    if (enetif->q_output_len==0) EM_ASSERT(7);// ASSERT: сбой подсчета размера очереди.
    enetif->q_output_len--;
    enetif->dgrams_out++;
    ETH_MEM_UNPROTECT();
    queue->next=NULL;
    queue->state=st_sending;
    struct ETH_PBUF* xpbuf=queue->pbuf;
    eth_queue_entry_free(queue);
    return xpbuf;
    }
  if (enetif->callback_fn) enetif->callback_pending=1;
  ETH_MEM_UNPROTECT();
  return NULL;
  }

void eth_add_packet_to_output_queue(struct ETH_QUEUE* queue, struct ETH_NETIF* enetif) {// для роутера. отправить в исходящую очередь нужного интерфейса (или передать сразу драйверу, если он ждёт пакет)
  queue->next = NULL;
  if (queue->pbuf->pbuf.ref==0) EM_ASSERT(8);
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  if (enetif->callback_pending) {// интерфейс ожидает блок - отправляем сразу в интерфейс без очереди
    if (enetif->q_output_end) EM_ASSERT(9);// assert: драйвер ждёт пакет, но в очереди почему-то что-то есть. Хотя должно быть пусто...
    enetif->callback_pending=0;
    queue->state = st_sending;
    enetif->dgrams_out++;
    ETH_MEM_UNPROTECT();
    struct ETH_PBUF* xpbuf=queue->pbuf;
    eth_queue_entry_free(queue);
    if (enetif->callback_fn) enetif->callback_fn(xpbuf, enetif);
    else EM_ASSERT(10);// no callback
    return;
    }
  if (enetif->q_output_len>=enetif->q_output_len_max) {// очередь уже полна. дропаем пакет.
    enetif->dgrams_lost++;
    ETH_MEM_UNPROTECT();
    eth_pbuf_free_request(queue->pbuf);
    eth_queue_entry_free(queue);
    return;
    }
  queue->state = st_in_queue;// добавляем в очередь
  if (enetif->q_output_end) enetif->q_output_end->next=queue; else enetif->q_output_begin=queue;// этот элемент приписываем к концу очереди
  enetif->q_output_end=queue;// конечным элементом ставим этот элемент
  enetif->q_output_len++;
  if (enetif->q_output_len_max<enetif->q_output_len) enetif->q_output_len_max=enetif->q_output_len;
  ETH_MEM_UNPROTECT();
  }

struct ETH_QUEUE* eth_mem_clone(struct ETH_QUEUE* queue) {
  struct ETH_QUEUE* queue_copy=eth_queue_entry_alloc(queue->pool);
  if (queue_copy) {
    queue_copy->pbuf=queue->pbuf;
    ETH_MEM_DECL_PROTECT();
    ETH_MEM_PROTECT();
    queue->pbuf->pbuf.ref++;
    ETH_MEM_UNPROTECT();
    }
  return queue_copy;
  }
