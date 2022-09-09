#include "eth_mem.h"
#include "eth_queue.h"

void add_to_custom_queue(struct pbuf* p, struct ETH_Q* q) {
  p->next = NULL;
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  if (q->q_len>=q->q_len_max) {// очередь уже полна. дропаем пакет.
    q->lost++;
    ETH_MEM_UNPROTECT();
    pbuf_free(p);
    return;
    }
  if (q->q_end) q->q_end->next=p; else q->q_begin=p;// этот элемент приписываем к концу очереди
  q->q_end=p;// конечным элементом ставим этот элемент
  q->q_len++;
  if (q->q_len_max<q->q_len) q->q_len_max=q->q_len;
  ETH_MEM_UNPROTECT();
  }

struct pbuf* get_from_custom_queue(struct ETH_Q* q) {
  ETH_MEM_DECL_PROTECT();
  ETH_MEM_PROTECT();
  struct pbuf* p=(struct pbuf*)q->q_begin;
  if (p==NULL) { ETH_MEM_UNPROTECT(); return NULL; }
  struct pbuf* next_pbuf=p->next;
  q->q_begin=next_pbuf;
  if (next_pbuf == NULL) q->q_end = NULL;
  if (q->q_len==0) EM_ASSERT(7);// ASSERT: сбой подсчета размера очереди.
  q->q_len--;
  ETH_MEM_UNPROTECT();
  p->next=NULL;
  return p;
  }
