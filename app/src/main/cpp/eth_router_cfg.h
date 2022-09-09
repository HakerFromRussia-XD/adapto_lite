#pragma once

#define ETH_MEM_BUFS_CNT 100 // количество буферов (занимают ETH_MEM_BUF_SIZE байт)
#define ETH_PBUFS_CNT 400// (занимают один pbuf)
#define ETH_MEM_BUF_SIZE 1600// размер данных ethernet пакета (начиная с мак адресов)
#define ETH_NETIFS 4// макс. количество интерфейсов

// номер интерфейса в роутере (начиная с единицы)
#define LWIP_NETIF_ID 1
#define TEST_NETIF_ID 2
#define BLE_NETIF_ID 3

// Зармеры исходящих очередей на интерфейсы
#define LWIP_NETIF_QLEN 20
#define UART_NETIF_QLEN 20
#define BLE_NETIF_QLEN 20
#define IN_BLE_NETIF_QLEN_THR 3
#define IN_UART_NETIF_QLEN_THR 3
