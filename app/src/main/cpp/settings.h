#pragma once

struct SETTINGS {
  uint32_t header;
  uint32_t checksum;
  uint32_t sequence_id;
  uint32_t flags1;
  uint32_t flags2;

  uint16_t thr_offset;
  uint16_t brk_offset;
  uint16_t thr_scale;
  uint16_t brk_scale; 
  uint16_t  handle_lin[3][33];
  uint16_t  handle_func[3][33];
  uint8_t  thr_progression;
  uint8_t  brk_progression;

  uint8_t netbios_name[24];
  uint8_t ipv4[4];// ipv4
  uint8_t gateway[4];// ipv4
  uint8_t ipv6[16];// ipv6
  uint8_t paired_mac_addr[6];// send debug info from this mac
  uint8_t mac_addr[6];// own mac address
  uint8_t ip_flags;// 1: IP enable, 2: DHCP enable
  uint8_t netmask;
  uint32_t serial;
  };
extern struct SETTINGS settings;
