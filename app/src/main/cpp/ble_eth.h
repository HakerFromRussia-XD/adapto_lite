#pragma once
#include <stdint.h>
#define MAX_CHUNK_LEN 167

struct BLE_ETH_STATS {
    uint32_t tx_chunks;
    uint32_t rx_chunks;
    uint32_t tx_dgrams;
    uint32_t rx_dgrams;

    uint32_t rx_too_short_chunk;
    uint32_t rx_too_short_dgram;
    uint32_t rx_oversize;
    uint32_t rx_pbuf_alloc_err;
    uint32_t rx_qpause;
    uint32_t rx_seqerr;

    uint32_t d1;
    uint32_t d2;
    uint32_t d3;
    uint32_t d4;
};

extern struct BLE_ETH_STATS ble_eth_stats;

int get_next_chunk(uint8_t* chunk_for_tx);
int process_dgram_asm(uint8_t* data, uint16_t len);
void eth_ble_if_init();
void gatt_eth_reset();
