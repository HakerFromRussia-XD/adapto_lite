struct ETH_Q {
  struct pbuf* q_begin;
  struct pbuf* q_end;
  uint32_t q_len;
  uint32_t q_len_max;
  uint32_t lost;
  };

// добавить ETH_PBUF p в очередь q
void add_to_custom_queue(struct pbuf* p, struct ETH_Q* q);


// возврат - NULL - очередь пуста; иначе pbuf
struct pbuf* get_from_custom_queue(struct ETH_Q* q);
