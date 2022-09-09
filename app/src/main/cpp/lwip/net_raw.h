#include "lwip/opt.h"
#include "lwip/netif.h"

// отдача принятых не-IP пакетов нужному обработчику, проверка контрольной суммы.
// отправка не IP кодограмм и формирование контрольной суммы


#define ETHTYPE_ADP 0x8777// braodcast топологии
#define ETHTYPE_DBG 0x8778// unicast diagram
#define ETHTYPE_CONTROLS 0x8779// controls, terminal, gui controls

struct eth_raw_frame {
  uint8_t dst_mac[6];
  uint8_t src_mac[6];
  uint8_t ethtype[2];// little endian везде в езернете. так что переворачиваем байты
  uint16_t csum;
  uint16_t len;
  uint8_t payload[1482];// +18
  };

void raw_send_bcast(struct eth_raw_frame* rawdgram);
void raw_send_ucast(struct eth_raw_frame* rawdgram);

err_t ethernet_send_raw(struct eth_raw_frame* eth_dgram);// кодограмма должна находиться в памяти доступной ETH периферии.
err_t ethernet_send_raw_to_paired(struct eth_raw_frame* dgrambuf);
