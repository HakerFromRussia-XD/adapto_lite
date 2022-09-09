#include <string.h>
//#include "main.h"
#include "lwip/lwip.h"
#include "lwip/net_raw.h"
#include "lwip/net_topology.h"
//#include "lwip/net_controls.h"
//#include "lwip/timeouts.h"
//#include "lwip/memp.h"
//#include "lwip/stats.h"
//#include "lwip/netif.h"
//#include "lwip/ethernetif.h"



void raw_send_ucast(struct eth_raw_frame* rawdgram) {
  rawdgram->ethtype[0]=ETHTYPE_ADP>>8;// eth type (88CD - SERCOS)
  rawdgram->ethtype[1]=(uint8_t)ETHTYPE_ADP;

//  if (len<62) len=62;
  ethernet_send_raw(rawdgram);
  }

void raw_send_bcast(struct eth_raw_frame* rawdgram) {
  rawdgram->dst_mac[0]=0xFF;// dst broadcast
  rawdgram->dst_mac[1]=0xFF;
  rawdgram->dst_mac[2]=0xFF;
  rawdgram->dst_mac[3]=0xFF;
  rawdgram->dst_mac[4]=0xFF;
  rawdgram->dst_mac[5]=0xFF;

  rawdgram->ethtype[0]=ETHTYPE_ADP>>8;// eth type (88CD - SERCOS)
  rawdgram->ethtype[1]=(uint8_t)ETHTYPE_ADP;

//  if (len<62) len=62;
  ethernet_send_raw(rawdgram);
  }



int dg_check(struct eth_raw_frame* dg, int len) {
  if (dg->len+18>len) return -1;// incorrect size
  uint16_t cs=0x2345+dg->len;
  for (int i=0; i<dg->len-1; i+=2) cs+=*(uint16_t*)&(dg->payload[i]);
  if (dg->len&1) cs+=dg->payload[dg->len-1];
  if (dg->csum!=cs) return -2;
  return 0;
  }

// ========================== custom proto rx handler
err_t lwip_customproto(struct pbuf *pb, struct netif *netif) {// input: 14byte header (+0-dst mac), +14: data [>=46 bytes], 4 bytes crc (L2)
  pb = pbuf_coalesce(pb, PBUF_RAW);
  if (pb->next != NULL) return ERR_IF;
  struct eth_raw_frame *ethhdr;
  ethhdr = (struct eth_raw_frame *)pb->payload;

  uint16_t ethtype=(ethhdr->ethtype[0]<<8)|ethhdr->ethtype[1];
  if (ethtype==ETHTYPE_DBG) {// dbg diagnostic data
    if (dg_check(ethhdr, pb->len)) return ERR_IF;

//    if (ethhdr->payload[0]==2) dbg_new_dgram((struct StrDBGData*)&(ethhdr->payload[0]), (uint8_t*)&(ethhdr->src_mac[0]), ethhdr->len);
//    if (ethhdr->payload[0]==0x33) ugr_new_dgram(ethhdr);
    pbuf_free(pb);
    return ERR_OK;
    }

  if (ethtype==ETHTYPE_ADP) {// topology
    if (dg_check(ethhdr, pb->len)) return ERR_IF;

    if (ethhdr->payload[0]==0) topology_new_dgram((struct StrNetTopData*)&(ethhdr->payload[2]), (uint8_t*)&(ethhdr->src_mac[0]), ethhdr->len);
    if (ethhdr->payload[0]==6) topology_disconnect((uint8_t*)&(ethhdr->src_mac[0]));
//    if (ethhdr->payload[0]==0x80) dfu_status_new_dgram(ethhdr);
//    if (ethhdr->payload[0]==0xFE) dfu_request_new_dgram(ethhdr);
    pbuf_free(pb);
    return ERR_OK;
    }

  if (ethtype==ETHTYPE_CONTROLS) {// controls (terminal, menu)
    if (dg_check(ethhdr, pb->len)) return ERR_IF;
//    controls_new_dgram(ethhdr);
    pbuf_free(pb);
    return ERR_OK;
    }
  return ERR_IF;
  }



err_t ethernet_send_raw(struct eth_raw_frame* eth_dgram) {
  struct pbuf p;
  memset(&p,0,sizeof(p));

  uint16_t dgram_len=eth_dgram->len;

  uint16_t cs=0x2345+dgram_len;
  for (int i=0; i<dgram_len-1; i+=2) cs+=*(uint16_t*)&(eth_dgram->payload[i]);
  if (dgram_len&1) cs+=eth_dgram->payload[dgram_len-1];

  dgram_len+=18;
  if (dgram_len<62) dgram_len=62;
  eth_dgram->csum=cs;

  err_t res=ERR_IF;
  p.payload = eth_dgram;
  p.len = dgram_len;
  p.tot_len = dgram_len;
  memcpy(eth_dgram->src_mac, &gnetif.hwaddr, 6);// source address
#include "lwip/eth_router_if_lwip.h"
  if (lwip_netif->linkoutput) {
    res=lwip_netif->linkoutput(lwip_netif, &p);
    }

  return res;
  }

err_t ethernet_send_raw_to_paired(struct eth_raw_frame* dgrambuf) {
    for (int n=0; n<paired_controllers_cnt; n++) {
      for (int j=0; j<6; j++) dgrambuf->dst_mac[j]=paired_controllers[n][j];
      int mac_ok=0;
      for (int i=0; i<6; i++) if (dgrambuf->dst_mac[i]!=0) mac_ok=1;

      if (mac_ok) ethernet_send_raw(dgrambuf);
      }
  return 0;
  }
