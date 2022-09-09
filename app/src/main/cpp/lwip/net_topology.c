// Формирование топологии устройств в сети

#include "string.h"
#include "lwip/sockets.h"
#include "lwip/api.h"
#include "lwip/tcpip.h"
#include "lwip/net_raw.h"
#include "net_topology.h"
#include "settings.h"

extern struct netif gnetif;

uint8_t net_devices=0;
struct StrDevices_online Devices_online[256];

uint8_t paired_controllers[256][6];// mac адреса запаренных контроллеров (список рассылки управления)
uint16_t paired_controllers_cnt;// mac адреса

static int nt_tmr=0;

static int mac_cmp(uint8_t* mac1, uint8_t* mac2) {
  if (mac1[0]!=mac2[0]) return 1;
  if (mac1[1]!=mac2[1]) return 1;
  if (mac1[2]!=mac2[2]) return 1;
  if (mac1[3]!=mac2[3]) return 1;
  if (mac1[4]!=mac2[4]) return 1;
  if (mac1[5]!=mac2[5]) return 1;
  return 0;
  }

static void fill_cell(struct StrNetTopData* yBuf, int i) {
  Devices_online[i].status=1;
  Devices_online[i].time=xTaskGetTickCount()+2000;
  memcpy(&Devices_online[i].serial, yBuf, sizeof(struct StrNetTopData));
  }

void topology_disconnect(uint8_t* mac) {// запрос на отключение устройства (контроллер просит забыть паринг, отклонил соединение)
  memset((uint8_t*)settings.paired_mac_addr,0,6);// unpair active device
  }

#pragma location="DRAM"
struct eth_raw_frame rawdgram3;
void topology_request_pair(uint8_t* mac) {// запрос на паринг или смену запаренного устройства
  rawdgram3.ethtype[0]=ETHTYPE_ADP>>8;// eth type (88CD - SERCOS)
  rawdgram3.ethtype[1]=(uint8_t)ETHTYPE_ADP;
  rawdgram3.payload[0]=0x05;// cmd (re-pair request)
  rawdgram3.payload[1]=0x00;// cmd2
  memcpy(&rawdgram3.dst_mac[0], mac, 6);

  rawdgram3.len=2;
  ethernet_send_raw(&rawdgram3);
  }

void topology_request_unpair(uint8_t* mac) {// запрос на паринг или смену запаренного устройства
  rawdgram3.ethtype[0]=ETHTYPE_ADP>>8;// eth type (88CD - SERCOS)
  rawdgram3.ethtype[1]=(uint8_t)ETHTYPE_ADP;
  rawdgram3.payload[0]=0x05;// cmd (re-pair request)
  rawdgram3.payload[1]=0x01;// cmd2
  memcpy(&rawdgram3.dst_mac[0], mac, 6);

  rawdgram3.len=2;
  ethernet_send_raw(&rawdgram3);
  }

void topology_new_dgram(struct StrNetTopData* yBuf, uint8_t* mac, int len) {
  if (len!=sizeof(struct StrNetTopData)+16) return;// size mismatch
  if (mac_cmp(mac, gnetif.hwaddr)==0) return;// own dgram

  int free_cell=-1;
  for (int i=0; i<net_devices; i++) {
    if (Devices_online[i].status==0) if (free_cell==-1) free_cell=i;
    if (mac_cmp(&mac[0], &Devices_online[i].mac[0])==0) {// update record
      fill_cell(yBuf, i);
      return;
      }
    }
  if (free_cell==-1) {
    if (net_devices<255) { free_cell=net_devices; net_devices++; } else return;
    }
  fill_cell(yBuf, free_cell);
  if (free_cell>=net_devices) net_devices=free_cell+1;
  for (int i=0; i<6; i++) Devices_online[free_cell].mac[i]=mac[i];
  }


void topo_periodic() {
  if ((int)(xTaskGetTickCount()-nt_tmr)>0) {
// убираем устройства, давно не активные

    nt_tmr=xTaskGetTickCount()+500;

    int d=net_devices;
    for (int i=0; i<d; i++) {
        if ((int32_t)(Devices_online[i].time-xTaskGetTickCount())<0) Devices_online[i].status=0;
//        return;
        }
    if (d>0) if (Devices_online[d-1].status==0) { d--; net_devices=d; }

    int dr=0;
    for (int i=0; i<d; i++) {
        if (Devices_online[i].status) {// формируем список управляемых дисплеем контроллеров
          if (mac_cmp(&Devices_online[i].paired_mac[0], gnetif.hwaddr)==0 && Devices_online[i].dev_class==1) {// контроллер привязан к этому дисплею
            for (int j=0; j<6; j++) paired_controllers[dr][j]=Devices_online[i].mac[j];
            dr++;
            }
          }
        }
    paired_controllers_cnt=dr;
    }
  }

#pragma location="ARAM"
struct eth_raw_frame rawdgram;
#pragma location="ARAM"
struct eth_raw_frame rawdgram2;

void topology_send_update(struct StrNetTopData* d, uint8_t* mac) {// Отправить запрос на обновление конфигурации устройства
  rawdgram2.ethtype[0]=ETHTYPE_ADP>>8;// eth type (88CD - SERCOS)
  rawdgram2.ethtype[1]=(uint8_t)ETHTYPE_ADP;
  rawdgram2.payload[0]=0x01;// cmd (update header)
  rawdgram2.payload[1]=0x00;// cmd2
  memcpy(&rawdgram2.payload[2], d, sizeof(struct StrNetTopData));
  memcpy(&rawdgram2.dst_mac[0], mac, 6);

  rawdgram2.len=sizeof(struct StrNetTopData)+16;
  ethernet_send_raw(&rawdgram2);
  }

void topology_broadcast_dgram() {
  struct StrNetTopData* s=(void*)&rawdgram.payload[2];
  rawdgram.payload[0]=0x00;// cmd (header)
  rawdgram.payload[1]=0x00;// cmd2 (header)
  s->serial=settings.serial;
  s->fw_ver[0]=0;
  s->fw_ver[1]=0;
  for (int i=0; i<6; i++) s->paired_mac[i]=settings.paired_mac_addr[i];
  for (int i=0; i<16; i++) s->name[i]=settings.netbios_name[i];
  s->ip_proto_enable=settings.ip_flags&1;
  s->flag_dhcp_enable=(settings.ip_flags>>1)&1;
  for (int i=0; i<4; i++) s->ip[i]=settings.ipv4[i];
  for (int i=0; i<4; i++) s->gw[i]=settings.gateway[i];
  s->netmask=settings.netmask;
  s->dev_class=2;// LCD
  rawdgram.len=sizeof(struct StrNetTopData)+16;
  raw_send_bcast(&rawdgram);
  }



/* =============================================================
Дисциплина обмена
1. broadcast / discovery.
Каждое устройство шлет broadcast с информацией о себе. (0.5сек интервал)
И каждое устройство собирает эти броадкасты. Если пакета нет 2 секунды, устройство исключается из списков.
Таким образом, у каждого устройства есть все устройства в сети.
по умолчанию настройка - dhcp + autoip

На старте одновременно включается и autoip и dhcp. Если DHCP сервер ответил, отключаем autoip.
Когда IP присвоен, 10 секунд проверочный интервал. Если не отвечает - проверочный интервал 1 секунда. 3 неудачные попытки - fallback на autoip.

Возможны следующие конфигурации:
- static IP / mask / GW
- DHCP + autoip

2. Настройка параметров
Контроллер пассивный. Дисплей шлёт в контроллер запросы: получение данных и обновление данных
В авто режиме для настройки параметров главным выбирается первый найденный.
В ручном - 

Протоколы работающие через IP:

3. Дебаг
Контроллер периодически запрашивает пакеты (группу пакетов).
Контролер может запросить информацию о текущем окне (номер последнего пакета, размер буфера и кол-во валидных пакетов в очереди)

5. Автонастройка (производство)
Если в контроллере нет мака и серийника, то контроллер присваевает случайный мак и стучится на локальный сервер с запросом присвоения мака и серийника.
Сервер выделяет мак-серийник, одновременно создает учетку, сохраняет туда серийный номер процессора, время и дату присвоения.
+ ssl ключи

6. Терминал (old style)
132*64 B&W (text mode: 22x8)


Коммуникация контроллера с сервером:
1. мониторинг и блокировка ?
2. настройка и управление параметрами ?
3. терминал
4. обновление прошивки
5. streaming debug (from controller)

Коммуникация дисплея с сервером:
1. debug data from SD (c LCD)
2. remote LCD screen (через сервер удаленное подключение к дисплею)

  ==============================================

- Подписка на debug info
  пока дисплей шлет запросы подписки, контроллер шлет дебаг инфу. Дебаг может слать только один контроллер.

- подписка на ручки газа-тормоза.
  может быть подписано любое количество контроллеров. контроллер сохраняет mac дисплея, на который подписан.
  При network discovery дисплей смотрит какие контроллеры в онлайне подписаны на газтормоз и шлет им всем данные.

- паринг для настрйоки и терминала
  дисплей запоминает с каким контроллером запарились. и шлёт это в discovery.
  контроллер в discovery смотрит какие дисплеи запарены и активны. и шлёт им обновления, а также дескрипторы (каталог) обновлений
  при принятии обновленных данных обновляет seq_id, дублирует seq_id.


   =============================================================== */
