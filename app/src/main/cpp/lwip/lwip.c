#include "lwip.h"
#include "lwip/init.h"
#include "lwip/netif.h"
#include "lwip/eth_router_if_lwip.h"
#include "lwip/dhcp_server.h"
#include "lwip/dnsproxy.h"
#include "eth_nat.h"

struct netif gnetif;
ip4_addr_t ipaddr;
ip4_addr_t netmask;
ip4_addr_t gw;

sem_t lwip_core_sem;

static void ethernet_link_status_updated(struct netif *netif) { }

#define DHCPS_SERVER_PORT 67

struct udp_pcb* pcb_dhcps;



static void dhcps_cb(void* arg, uint8_t ip[4], uint8_t mac[6])
{
/*    esp_netif_t netif = arg;
    ESP_LOGD(TAG, "%s esp_netif:%p", __func__, esp_netif);
    ip_event_ap_staipassigned_t evt = { .esp_netif = esp_netif };
    memcpy((char *)&evt.ip.addr, (char *)ip, sizeof(evt.ip.addr));
    memcpy((char *)&evt.mac, mac, sizeof(evt.mac));
    ESP_LOGI(TAG, "DHCP server assigned IP to a client, IP is: " IPSTR, IP2STR(&evt.ip));
    ESP_LOGD(TAG, "Client's MAC: %x:%x:%x:%x:%x:%x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    int ret = esp_event_post(IP_EVENT, IP_EVENT_AP_STAIPASSIGNED, &evt, sizeof(evt), 0);
    if (ESP_OK != ret) {
        ESP_LOGE(TAG, "dhcps cb: failed to post IP_EVENT_AP_STAIPASSIGNED (%x)", ret);
    }*/
}


void MX_LWIP_Init(void) {

  sem_init(&lwip_core_sem, 0, 1);

  LOCK_LWIP_CORE();
  lwip_init();

  ipaddr.addr =0;
  netmask.addr = 0;
  gw.addr = 0;

  IP4_ADDR(&ipaddr,  192, 168, 245, 1);
  IP4_ADDR(&netmask, 255, 255, 255, 0);
  IP4_ADDR(&gw,      192, 168, 245, 1);

  netif_add(&gnetif, &ipaddr, &netmask, &gw, NULL, &eth_lwip_if_init, &ethernet_input);// with router
  netif_set_default(&gnetif);
  netif_set_up(&gnetif);
  netif_set_link_callback(&gnetif, ethernet_link_status_updated);
  sys_restart_timeouts();

  dhcps_t* dhcpd=dhcps_new();
  dhcps_dns_setserver(dhcpd, &ipaddr);
  if (dhcpd) dhcps_start(dhcpd, &gnetif, ipaddr);

  dnsproxy_init();

  eth_nat_init();

//  IP4_ADDR(&broadcast_dhcps, 255, 255, 255, 255);
//  server_address = info->ip;
//  wifi_softap_init_dhcps_lease(server_address.addr);
//  client_address_plus.addr = dhcps_lease.start_ip.addr;
//  udp_bind(pcb_dhcps, IP_ADDR_ANY, DHCPS_SERVER_PORT);
//  udp_recv(pcb_dhcps, handle_dhcp, NULL);

  UNLOCK_LWIP_CORE();
  }

