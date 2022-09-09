#ifndef TESTBLE_BT_DRV_H
#define TESTBLE_BT_DRV_H

#include <jni.h>

int bt_send(uint8_t* dgr, uint16_t dglen);
void send_thread_register();
extern volatile int ble_connection_status;
#endif //TESTBLE_BT_DRV_H
