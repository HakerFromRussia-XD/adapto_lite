struct BLE_TCP_STATS {
  uint32_t tx_raw;
  uint32_t rx_raw;
  uint32_t tx_blocks;
  uint32_t rx_blocks;
  uint32_t tx_err;
  uint32_t tx_ack_err;
  uint32_t tx_sack_err;
  uint32_t tx_ack_negdist_err;
  uint32_t tx_ack_toomuch_err;

  uint32_t ack_upd;
  uint32_t rx_empty_dg;
  uint32_t rx_only_ack;
  uint32_t rx_seq_err;
  uint32_t rx_dup_ack;
  uint32_t rx_neg_ack;
  uint32_t rx_duplicates;
  uint32_t rx_detected_retransmit_jumps;
  uint32_t rx_detected_packet_loss_single;
  uint32_t rx_detected_packet_loss_multiple;
  uint32_t rx_detected_incorrect_jumps;

  uint32_t rst_cnt;
  uint32_t rstreq_cnt;
  uint32_t rxasm_throttle;
  uint32_t rxasm_try;
  uint32_t tx_bytes;
  uint32_t rx_bytes;
  uint32_t d1;
  uint32_t d2;
  uint32_t d3;
  uint32_t d4;
  };
extern struct BLE_TCP_STATS ble_tcp_stats;

struct BLE_TCP {
  uint32_t ble_tx_busy;
  };
extern struct BLE_TCP ble_tcp;


extern uint8_t tx_busy_mask[8];
extern uint8_t rx_ready_mask[8];
extern uint8_t rx_nack_blk;
extern uint8_t rx_last_recvd_blk;

extern uint8_t tx_ack_blk;// следующая после подтвержденной ячейка (т.е. начиная с которой надо передавать)
extern uint8_t tx_index_blk;// ячейка ожидающая передачу
extern uint8_t tx_push_ptr_blk;// ячейка ожидающая добавление элемента

void ble_tcp_init();

void notify_ble_out_queue();// 
void ble_tx_complete(int status);
void process_gatt_data(uint8_t* data, uint16_t len);
void gatt_conn_reset();
