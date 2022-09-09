#pragma once

#include "lwip/api.h"

extern uint8_t paired_controllers[256][6];// mac адреса
extern uint16_t paired_controllers_cnt;// mac адреса

struct StrDBGData {
  uint16_t cmd;
  uint16_t seq;
  uint16_t len;
  uint8_t data[1472];// 32*46
  };

struct StrNetTopData {
  uint32_t serial;// серийник устройства
  uint8_t fw_ver[2];// firmware version (x.y)
  uint8_t paired_mac[6];// mac последнего сопреженного с ним устройства (если это контроллер)
  uint8_t name[16];// название устройства (16 chars max)
  uint8_t ip_proto_enable:1;// включение ip протокола (dhcp, ip адрес и прочее). без ip работа только локально с дисплеем.
  uint8_t flag_dhcp_enable:1;// ip автоматический или статический
  uint8_t netmask;// 0-32: /0 - /32
  uint8_t ip[4];// ip address
  uint8_t gw[4];// gateway
  uint16_t dev_class;// 1 - controller, 2 - lcd
  };

struct StrDevices_online {
  uint32_t time;// время последней активности устройства
  uint8_t status;// 0 - пустая ячейка, 1 - активное устройство в текущий момент
  uint8_t mac[6];// mac устройства
  uint32_t serial;// серийник устройства
  uint8_t fw_ver[2];// firmware version (x.y)
  uint8_t paired_mac[6];// mac последнего сопреженного с ним устройства (если это контроллер)
  uint8_t name[16];// название устройства (16 chars max)
  uint8_t ip_proto_enable:1;// включение ip протокола (dhcp, ip адрес и прочее). без ip работа только локально с дисплеем.
  uint8_t flag_dhcp_enable:1;// ip автоматический или статический
  uint8_t netmask;// 0-32: /0 - /32
  uint8_t ip[4];// ip address
  uint8_t gw[4];// gateway
  uint16_t dev_class;// 1 - controller, 2 - lcd
  };

extern struct StrDevices_online Devices_online[256];

void topology_disconnect(uint8_t* mac);
void topology_new_dgram(struct StrNetTopData* yBuf, uint8_t* mac, int len);
void topo_periodic();
void topology_broadcast_dgram();
void topology_send_update(struct StrNetTopData* d, uint8_t* mac);// Отправить запрос на обновление конфигурации устройства
void topology_request_pair(uint8_t* mac);
void topology_request_unpair(uint8_t* mac);
