#pragma once

#include <semaphore.h>
#include "xlib.h"
#include "eth_router_cfg.h"
#include "lwip/pbuf.h"

/* Менеджер памяти и очередей для ethernet пакетов.
Умеет:
- выделять и освобождать память из пулов (пока пул один, но архитектурно заложена возможность использования нескольких пулов)
- при ошибке выделения памяти вызвать callback как только память появилась (т.е. как только кто-то освободил)
- работать с очередями: помещать пакеты из всех интерфейсов в общую очередь на роутинг (из irq или потока)
Менеджер содержит в себе поток, обслуживающий нужды очереди менеджера памяти и очередь на роутинг. Из этого потока вызываются все коллбэки.
Коллбэки могут заниматься среднезатратными по времени вещами (например, копирование всего пакета или подсчет CRC всего пакета), но не могут вставать в ожидание чего-либо.
Ожидание коллбэков будет тормозить обработку входящих очередей с интерфейсов и очереди на операции с памятью.

Роутер из очереди на роутинг распихивает пакеты в интерфейсы назначения. Если при попытке запихнуть пакет в интерфейс назначения размер очередь этого интерфейса заполнена, пакет отбрасывается.
Для броадкастов роутером создаются копии пакета на каждый исходящий интерфейс. Ибо нужно каждую копию запихнуть в свою очередь, а очередь у пакета только одна.
Рекомендуемый размер входящих очередей - 2 пакета: один в процессе приёма, второй в потоке роутера.
Пакеты во входящих очередях обрабатываются сразу (как это позволть сделать приоритет потока обработчика).
Размер исходящих очередей должен быть больше, его задача удерживать в буферах всё что вовремя не пролезает через медленные интерфейсы.
Пока пакет не фрагментируется. Всё что не влезает в размер одного буфера (ETH_MEM_BUF_SIZE), отбрасывается.


Рекомендации по драйверам интерфейсов:
- при инициализации:
eth_netif_array[YOUR_NETIF_ID].q_output_len_max=<размер очереди на передачу в пакетах>
eth_netif_array[YOUR_NETIF_ID].callback_fn=lwip_callback_from_bridge;// инициализируем callback (пакет готов)
eth_netif_array[ETH_NETIF_ID].if_up=1;// интерфейс готов принимать пакеты


- выделить память из пула перед приёмом пакета (т.е. буфера резервируются когда инициализируются буфера приёма), и записывать пакет сразу в эту память.
- если при инициализации буфера был отказ в выделении памяти, ждать коллбэк. Когда прилетит callback, можно инициализировать буфер и запускать приём.

Передача:
1. инициализируем callback на новый пакет.
2. запрашиваем пакет (eth_get_packet). Функция либо возвратит что есть в очереди, либо активирует коллбэк. В этом случае ждем пакет.
3. когда пакет получен, передаем пакет
4. повторяем начиная с пп.2
Обработчик коллбэка вызывается один раз после того как eth_get_packet вернул NULL (т.е. отсутствие пакета)
*/

// параметры инициализации пула по умолчанию:
extern sem_t eth_mem_sem;


#define ETH_MEM_ALIGN __attribute__((aligned(4)))// для CPU без кеша

#define ETH_MEM_DECL_PROTECT()  
#define ETH_MEM_PROTECT()       {sem_wait(&eth_mem_sem); /*asm("DSB SY");*/}
#define ETH_MEM_UNPROTECT()     {/*asm("DSB SY");*/ sem_post(&eth_mem_sem);}

#define EM_ASSERT(x) {dbg[10]=x; while(1);}



// ======================== Управляющие структуры =============
struct ETH_PBUF_CTRL {
  volatile struct ETH_PBUF* free_chain;// linked-list для размещения-освобождения буферов
  volatile uint32_t used;// счетчик, сколько сейчас элементов занято
  uint32_t used_max;// сколько было занято максимум
  struct ETH_QUEUEPOOL_CTRL* pool;

  };

#ifndef PBUF_FLAG_IS_CUSTOM// cleck for LWIP pbuf is included
#define PBUF_FLAG_IS_CUSTOM 0x02U

struct pbuf {
  struct pbuf *next; // next fragment in chain (NULL - end of packet)
  void *payload;       // memory for packet payload
  uint16_t tot_len;       // remain packet length from begin of this part
  uint16_t len;           // this fragment length
  uint8_t type_internal;  // payload directly follows the struct pbuf
  uint8_t flags;          // PBUF_FLAG_IS_CUSTOM
  uint8_t ref; // reference count
  uint8_t if_idx;         // network interface index
  };

typedef void (*pbuf_free_custom_fn)(struct pbuf *p);
#endif

struct ETH_PBUF {
  struct pbuf pbuf;
   // struct pbuf:   *next // next fragment in chain (NULL - end of packet)
   // void *payload;       // memory for packet payload
   // u16_t tot_len;       // remain packet length from begin of this part
   // u16_t len;           // this fragment length
   // u8_t type_internal;  // payload directly follows the struct pbuf
   // u8_t flags;          // PBUF_FLAG_IS_CUSTOM
   // LWIP_PBUF_REF_T ref; // reference count
   // u8_t if_idx;         // network interface index
   // pbuf_free_custom_fn  // custom pbuf free function
  pbuf_free_custom_fn custom_free_function;
  uint8_t* default_payload;
  uint8_t src_if;
  struct ETH_PBUF_CTRL* ctrl;
  };


struct ETH_QUEUE {// ячейка пула
  struct ETH_QUEUE* next;// указатель на следующий элемент списка (очереди, которая определяется параметром mia_state)
  struct ETH_QUEUEPOOL_CTRL* pool;
  struct ETH_PBUF* pbuf;// Обращаться к ETH_PBUF / pbuf нужно через этот указатель!
  volatile enum {
    st_free=0,// ячейка свободна, поле next указывает на следующую свободную ячейку, т.е. используется дл выделения памяти
    st_allocated=1,// ячейка занята, идёт процесс записи данных в ячейку, поле next не используется
    st_in_queue=2,// ячейка в очереди на обработку, поле next указывает на следующую ячейку в данной очереди
    st_sending=3,// в процессе отправки на интерфейсе назначения
    st_free_pending=4 } state;// ячейка в очереди на освобождение (или передачи другому ожидающему память владельцу), поле next указывает на следующий элемент очереди на совобождение
  uint8_t dst_if_idx;// если 0 - то роутится роутером. Если не 0, то роутится в соответствующиё исходящий интерфейс
  };

struct ETH_MEM_STATS {
  uint32_t qpool_alloc_fails;// queue entry alloc fail (summary, for all queues)
  uint32_t eth_pbuf_alloc_fails;
  uint32_t eth_pbuf_alloc_delayed;
  uint32_t eth_pbuf_alloc_delay_cbks;
  };

extern struct ETH_MEM_STATS eth_mem_stats;

struct NOMEM_CALLBACK {// структура для организации очереди коллбэков на выделение памяти для входящих пакетов, т.е. принимаемых драйвером (экземпляры структуры создаются интерфейсом)
  void (*callback_fn)(struct ETH_PBUF*, struct NOMEM_CALLBACK*);
  struct NOMEM_CALLBACK* next_p;// chain for pbuf allocation requests
  struct ETH_PBUF* p;
  uint8_t src_interface_id;
  uint8_t wait_active;// Если в состоянии ожидания выдачи памяти - то =1
  };

struct ETH_NETIF {// сетевой интерфейс (элемент netif_list). определа в виде массива элементов заранее определенного размера, индекс является номером интерфейса и eth_netif_id.
  volatile struct ETH_QUEUE* q_output_end;// сюда добавляем пакет после роутинга
  volatile struct ETH_QUEUE* q_output_begin;// забираем пакет драйвером интерфейса
  volatile uint32_t q_output_len; // размер исходящей очереди
  volatile uint32_t q_output_len_max; // фиксируем пик очереди
  volatile uint32_t q_output_size; // максимальный размер исходящей очереди. Количество элементов в пуле должно хватать для обслуживания всех очередей.
// входящая очередь не лимитируется (предполагается, что пакет во входящей очереди не задерждится надолго и быстро улетит в исходящую, либо отбросится если исходящая очередь пепеполнена).

  void (*callback_fn)(struct ETH_PBUF*, struct ETH_NETIF*);// callback готовности пакета на передачу драйверу
  struct ETH_QUEUEPOOL_CTRL* pool;// пул памяти, использующийся этим интерфейсом
  void (*q_update_cbk)(struct ETH_NETIF*);// callback уменьшения кол-ва пакетов в ожидании обработки от этого интерфейса
  uint8_t eth_netif_id;// ID интерфейса. всегда является индексом eth_netif_array. сделан для ускорения определения индекса по указателю.
  volatile uint8_t if_up;// интерфейс включен (если 0, то интерфейс не используется для передачи-приема пакетов)
  volatile uint8_t callback_pending;// ждём callback
  uint32_t dgrams_in;
  uint32_t dgrams_out;
  uint32_t dgrams_lost;
  uint32_t input_queue_len;// размер очереди кодограмм от этого интерфейса
  char* if_name;
  };

struct ETH_QUEUEPOOL_CTRL {// описание пула элементов для интерфейса моста
  volatile struct ETH_QUEUE* q_begin;// начало списка свободных буферов. Сюда и добавляем освобождаемую память, и забираем при размещении (т.е. работает как стек)
  volatile struct NOMEM_CALLBACK* nopbuf_q_end;// сюда добавляем / очередь желающих получить pbuf
  volatile struct NOMEM_CALLBACK* nopbuf_q_begin;// отсюда забираем
  volatile struct ETH_PBUF* free_pbuf_q_end;// добавляем - очередь запросов на освобождение блока
  volatile struct ETH_PBUF* free_pbuf_q_begin;// забираем
  volatile struct ETH_QUEUE* q_input_end;// добавляем - очередь входящих пакетов на роутинг
  volatile struct ETH_QUEUE* q_input_begin;// забираем
  volatile uint32_t q_input_used;// счетчик, сколько сейчас элементов входящей очереди занято
  volatile uint32_t q_input_used_max;// пик
  volatile uint32_t used;// счетчик, сколько сейчас элементов пула занято
  uint32_t used_max;// сколько было занято максимум
  volatile uint32_t alloc_fails;// счетчик ошибок выделения
  struct ETH_NETIF* eth_netif_array;// массив структур описывающих подключенные сетевые интерфейсы
  struct NHandle_t tNotifyHandle;// процесс, обслуживающий этот пул
  pthread_t pThr;
  uint8_t eth_netif_size;// количество элементов структуры eth_netif_array
  };



// ======================== API =============

// проинициализировать мост:
// - кастомный пул памяти (pool - управляющая структура, pool_mem - структура блоков памяти для пула, pool_mem_size_blocks - количество блоков памяти)
// - пул очередей
// не thread-safe. пул можно переинициализировать в любой момент. Пока не завершена (пере)инициализация нельзя пользоваться функциями работы с пулом.
// т.е. сперва инициализируем пул, потом запускаем периферию интерфейсов.
void eth_router_add_bridge(struct ETH_QUEUEPOOL_CTRL* pool, struct ETH_QUEUE* eth_q, uint32_t pool_mem_size_blocks,
                           struct ETH_PBUF_CTRL* eth_pbuf_ctrl, struct ETH_PBUF* eth_pbuf, uint8_t* eth_pbuf_mem, uint32_t eth_pbufs_cnt,
                           struct ETH_NETIF* netif_arr, uint32_t netif_count);

void eth_router_start_task(struct ETH_QUEUEPOOL_CTRL* pool);

// Внутренняя функция
// выделить элемент очереди из пула pool (thread/irq safe, если настроено выше)
//struct ETH_QUEUE* eth_queue_entry_alloc(struct ETH_QUEUEPOOL_CTRL* pool);

// Функция для роутера
// Освобождаем блок (thread/irq safe, если настроено выше).
// Блок должен быть выделен из любого пула ETH_QUEUE.
void eth_queue_entry_free(struct ETH_QUEUE* q);

// Запрос пакета драйвером интерфейса из очереди (thread/irq safe, если настроено выше)
// Если пакет есть в очереди на передачу, пакет извлекается из очереди и указатель на него возвращается функцией.
// Драйвер должен передать пакет, затем освободить буфер.
// Если в очереди пусто, функция возвращает NULL и инициирует ожидание пакета в очереди. Как только в очереди появится новый пакет, будет вызван callback, описанный в структуре enetif.
// В вызове callback будет указатель на пакет и указатель на сетевой интерфейс.
// в enetif предварительно надо проинициализировать:
//   callback_fn - указатель на callback-функцию;
struct ETH_PBUF* eth_get_packet(struct ETH_NETIF* enetif);

// Функция для драйвера сетевого интерфейса
// отправить принятый интерфейсом пакет в роутер (thread/irq safe, если настроено выше)
// перед этим вызовом предполагается что память была выделена функцией eth_pbuf_alloc, пакет записан в память pbuf->payload и в pbuf->if_idx вписан номер интерфейса.
// размер доступной памяти payload в байтах равен ETH_MEM_BUF_SIZE
void eth_add_packet_to_router_queue(struct ETH_PBUF* pbuf);

// Функция для роутера
// отправить пакет "block" в очередь исходящую очередь интерфейса "enetif" (только из потока роутера).
// либо если драйвер ждёт пакет, педедать его сразу, минуя очередь
void eth_add_packet_to_output_queue(struct ETH_QUEUE* queue, struct ETH_NETIF* enetif);

// Функция для роутера
// дублирует пакет и создает новую очередь для пакета.
// память пакета для всех копий общая. Поэтому изменеять содержимое пакета после клонирования нельзя.
// используется для броадкастов (отправка копии пакета на все интерфйесы)
struct ETH_QUEUE* eth_mem_clone(struct ETH_QUEUE* block);

// выделяет pbuf. Если указан CB, то при ошибке выделения как только появится свободный ETH_PBUF, будет вызван callback с этим ETH_PBUF
// Для активации CB нужен заранее проинициализированный экземпляр структуры callback.
// Если предполагается очередь из нескольких callback-ов на отказ памяти, то для всех callback-ов, которые одновременно могут находиться в очереди, нужен отдельный экземпляр структуры.
// Рекомендуется при первом отказе памяти перестать размещать новые блоки и дождаться callback-а. В коллбэке можно пытаться дальше выделять память до первой ошибки.
// в cb предварительно надо проинициализировать:
//   callback_fn - указатель на callback-функцию;
//   src_interface_id - номер интерфейса (он проинициализируется в pbuf). Также можно использовать для определения экземпляра драйвера (если один обработчик на несколько интерфейсов)
// Нумерация интерфейсов - от единицы. 0 означает "не проинициализировано". Драйвер должен установить корректно номер интерфейса, иначе будут проблемы с роутингом.
// если cb=NULL, то номер интерфейса нужно будет проинициализировать самостоятельно после выделения блока.
struct ETH_PBUF* eth_pbuf_alloc(struct ETH_PBUF_CTRL* eth_pbuf_ctrl, struct NOMEM_CALLBACK* cb);

// Освобождаем блок (thread/irq safe, если настроено выше).
// Блок должен быть выделен из любого пула ETH_PBUF.
void eth_pbuf_free_request(struct ETH_PBUF* pbuf);
