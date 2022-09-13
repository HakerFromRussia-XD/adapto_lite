#include <jni.h>
#include "bt_drv.h"
#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <unistd.h>
#include <string.h>
#include <sched.h>
#include <semaphore.h>
#include <stdatomic.h>
#include "xlib.h"
#include "ble_tcp.h"
#include "eth_mem.h"
#include "eth_router.h"
#include "ble_eth.h"
#include "lwip/lwip.h"
#include "eth_nat.h"
//#include "lwip/opt.h"
//#include "lwip/mem.h"
//#include "lwip/memp.h"
//#include "lwip/netif.h"
//#include "lwip/init.h"

int dbg_scr_num=0;

// ====================================== startup =========================
JavaVM* gJvm = NULL;
jobject bleservice;
jclass bleserv;
jclass bleserv1;
JNIEnv* send_env;
JNIEnv* status_upd_env;

jmethodID send_method;
jmethodID status_upd_method;

volatile int ble_connection_status=0;// 2 - connection up.

int bt_send(uint8_t* dgr, uint16_t dglen) {
    JNIEnv *env=send_env;
    jbyteArray dgram=(*env)->NewByteArray(env, dglen);
//    jbyteArray dgram = env->NewByteArray(dglen);
    if (dgram == NULL) return -1;
    (*env)->SetByteArrayRegion (env, dgram, 0, dglen, dgr);

//    jbyteArray bytes = (*env)->GetObjectField(env, obj, sendId)
//    jbyte *stateJava = (*env)->GetByteArrayElements(env, dgram, NULL);
//    if (stateJava == NULL) return -1;
//    memcpy(stateJava, dgr, dglen);
    int ret=-1;
    if (send_method) ret=(*env)->CallStaticIntMethod(env, bleserv, send_method, dgram);
    (*env)->DeleteLocalRef(env, dgram);
    return ret;
}
jbyteArray jname;
jbyteArray jval;

static void status_upd(int p, char* name, char* val) {
    JNIEnv *env=status_upd_env;
    jbyteArray jname=(*env)->NewByteArray(env, strlen(name));
    if (jname == NULL) return;
    (*env)->SetByteArrayRegion (env, jname, 0, strlen(name), name);
    jbyteArray jval=(*env)->NewByteArray(env, strlen(val));
    if (jval == NULL) return;
    (*env)->SetByteArrayRegion (env, jval, 0, strlen(val), val);
    if (status_upd_method) (*env)->CallStaticVoidMethod(env, bleserv1, status_upd_method, p, jname, jval);
    (*env)->DeleteLocalRef(env, jname);
    (*env)->DeleteLocalRef(env, jval);
}

void status_upd_thread_register() {
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6; // choose your JNI version
    args.name = NULL;
    args.group = NULL;
    jint ret=(*gJvm)->AttachCurrentThread(gJvm, &status_upd_env, &args);
    if (ret) return;
}

void send_thread_register() {
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6; // choose your JNI version
    args.name = NULL;
    args.group = NULL;
    jint ret=(*gJvm)->AttachCurrentThread(gJvm, &send_env, &args);
    if (ret) return;
}

void status_upd1(int i, char* b, uint32_t c) {
    char str[100];
    sprintf(str, "%d",c);
    status_upd(i, b, str);
}


uint32_t tx_hist[16];
uint32_t rx_hist[16];
uint32_t htime[16];
uint8_t histptr;


pthread_t thr;
_Noreturn void status_thread(void* arg) {
    status_upd_thread_register();
    while(1) {
        int p=histptr&15;
        tx_hist[p]=ble_tcp_stats.tx_bytes;
        rx_hist[p]=ble_tcp_stats.rx_bytes;
        htime[p]=xTaskGetTickCount();
        int tx_spd=(tx_hist[p]-tx_hist[(p-10)&15])*1000/(htime[p]-htime[(p-10)&15]);
        int rx_spd=(rx_hist[p]-rx_hist[(p-10)&15])*1000/(htime[p]-htime[(p-10)&15]);
        p++;
        histptr=p&15;

        int i = 1;
        if (dbg_scr_num==0) {
            status_upd1(i++, "dbg[0]", dbg[0]);
            status_upd1(i++, "dbg[1]", dbg[1]);
            status_upd1(i++, "dbg[2]", dbg[2]);
            status_upd1(i++, "dbg[3]", dbg[3]);
            status_upd1(i++, "dbg[4]", dbg[4]);
            status_upd1(i++, "dbg[5]", dbg[5]);
            status_upd1(i++, "dbg[6]", dbg[6]);
            status_upd1(i++, "dbg[7]", dbg[7]);
            status_upd1(i++, "dbg[8]", dbg[8]);
            status_upd1(i++, "dbg[9]", dbg[9]);
            status_upd1(i++, "dbg[10]", dbg[10]);

            status_upd1(i++, "tx_chunks", ble_eth_stats.tx_chunks);
            status_upd1(i++, "rx_chunks", ble_eth_stats.rx_chunks);
            status_upd1(i++, "tx_dgrams", ble_eth_stats.tx_dgrams);
            status_upd1(i++, "rx_dgrams", ble_eth_stats.rx_dgrams);
            status_upd1(i++, "rx_qpause", ble_eth_stats.rx_qpause);
            status_upd1(i++, "rx_seqerr", ble_eth_stats.rx_seqerr);

            status_upd1(i++, "tx_spd", tx_spd);
            status_upd1(i++, "rx_spd", rx_spd);

        }
        if (dbg_scr_num==1) {
            status_upd1(i++, "ble_conn_status", ble_connection_status);
            status_upd1(i++, "BLEif_dg_in", eth_netif_array[BLE_NETIF_ID].dgrams_in);
            status_upd1(i++, "BLEif_dg_lost", eth_netif_array[BLE_NETIF_ID].dgrams_lost);
            status_upd1(i++, "BLEif_dg_out", eth_netif_array[BLE_NETIF_ID].dgrams_out);
            status_upd1(i++, "BLEif_dg_outqlen", eth_netif_array[BLE_NETIF_ID].q_output_len);
            status_upd1(i++, "BLEif_dg_in_qlen", eth_netif_array[BLE_NETIF_ID].input_queue_len);
            status_upd1(i++, "BLEif_cbk_p", eth_netif_array[BLE_NETIF_ID].callback_pending);

            status_upd(i++, "", "");
            status_upd1(i++, "LWif_dg_in", eth_netif_array[LWIP_NETIF_ID].dgrams_in);
            status_upd1(i++, "LWif_dg_lost", eth_netif_array[LWIP_NETIF_ID].dgrams_lost);
            status_upd1(i++, "LWif_dg_out", eth_netif_array[LWIP_NETIF_ID].dgrams_out);
            status_upd1(i++, "LWif_dg_outqlen", eth_netif_array[LWIP_NETIF_ID].q_output_len);
            status_upd1(i++, "LWif_dg_in_qlen", eth_netif_array[LWIP_NETIF_ID].input_queue_len);
            status_upd1(i++, "LWif_cbk_p", eth_netif_array[LWIP_NETIF_ID].callback_pending);

            status_upd(i++, "", "");
            status_upd1(i++, "NAT_pbuf_brk", nat_stat.pbuf_broken);
            status_upd1(i++, "NAT_tcp_new", nat_stat.tcp_new);
            status_upd1(i++, "NAT_tcp_close", nat_stat.tcp_close);

            status_upd(i++, "", "");
            status_upd1(i++, "eth_pbuf used", xeth_pbuf_ctrl.used);
            status_upd1(i++, "eth_queue used", xeth_queuepool_ctrl.used);
            status_upd1(i++, "nat pbuf used", nat_stat.pbuf_used);
            status_upd1(i++, "nat lw conn used", nat_stat.lw_conn_used);

        }
        if (dbg_scr_num==2) {
            status_upd1(i++, "tx_raw", ble_tcp_stats.tx_raw);
            status_upd1(i++, "rx_raw", ble_tcp_stats.rx_raw);
            status_upd1(i++, "tx_blocks", ble_tcp_stats.tx_blocks);
            status_upd1(i++, "rx_blocks", ble_tcp_stats.rx_blocks);
            status_upd1(i++, "tx_err", ble_tcp_stats.tx_err);
            status_upd1(i++, "tx_ack_err", ble_tcp_stats.tx_ack_err);
            status_upd1(i++, "tx_sack_err", ble_tcp_stats.tx_sack_err);
//            status_upd1(i++, "tx_ack_negd_err", ble_tcp_stats.tx_ack_negdist_err);
//            status_upd1(i++, "tx_ack_toom_err", ble_tcp_stats.tx_ack_toomuch_err);
//            status_upd1(i++, "ack_upd", ble_tcp_stats.ack_upd);
//            status_upd1(i++, "rx_empty_dg", ble_tcp_stats.rx_empty_dg);
//            status_upd1(i++, "rx_only_ack", ble_tcp_stats.rx_only_ack);
            status_upd1(i++, "rx_seq_err", ble_tcp_stats.rx_seq_err);
//            status_upd1(i++, "rx_dup_ack", ble_tcp_stats.rx_dup_ack);
//            status_upd1(i++, "rx_neg_ack", ble_tcp_stats.rx_neg_ack);
            status_upd1(i++, "rx_duplicates", ble_tcp_stats.rx_duplicates);
            status_upd1(i++, "rx_rtr_jmps", ble_tcp_stats.rx_detected_retransmit_jumps);
//            status_upd1(i++, "rx_pl1", ble_tcp_stats.rx_detected_packet_loss_single);
//            status_upd1(i++, "rx_pl2+", ble_tcp_stats.rx_detected_packet_loss_multiple);
            status_upd1(i++, "rx_incor_jmps", ble_tcp_stats.rx_detected_incorrect_jumps);
            char b[100]; int ptr=0;
//            for (int i=0; i<8; i++) ptr+=sprintf(b+ptr, "%d ",tx_busy_mask[i]);
            status_upd(i++, "tx_busy_mask", b);
//            status_upd1(i++, "tx_ack_blk", tx_ack_blk);
            status_upd1(i++, "tx_index_blk", tx_index_blk);
            status_upd1(i++, "tx_push_ptr_blk", tx_push_ptr_blk);
            ptr=0;
//            for (int i=0; i<8; i++) ptr+=sprintf(b+ptr, "%d ",rx_ready_mask[i]);
            status_upd(i++, "rx_ready_mask", b);
            status_upd1(i++, "rx_nack_blk", rx_nack_blk);
//            status_upd1(i++, "rx_last_rcvd_blk", rx_last_recvd_blk);
            status_upd1(i++, "conn_rst", ble_tcp_stats.rst_cnt);
            status_upd1(i++, "conn_rst_req", ble_tcp_stats.rstreq_cnt);
            status_upd1(i++, "ble_tx_busy", ble_tcp.ble_tx_busy);
            status_upd1(i++, "rxasm_try", ble_tcp_stats.rxasm_try);
            status_upd1(i++, "rxasm_throttle", ble_tcp_stats.rxasm_throttle);

        }
        while (i<=30) status_upd(i++, "", "");
        usleep(100000);
    }
}

JNIEXPORT void JNICALL
Java_ua_cn_stu_navigation_MainActivity_change_1dbg_1scr(JNIEnv *env, jobject thiz, jint scr) {
  dbg_scr_num=scr;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* pjvm, void* reserved) {
    gJvm = pjvm;
    JNIEnv* env;
    if ((*pjvm)->GetEnv(pjvm, &env, JNI_VERSION_1_6) != JNI_OK) {return JNI_ERR;}

    bleserv = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "ua/cn/stu/navigation/MainActivity"));
    if (bleserv == NULL) return JNI_ERR;
    bleserv1 = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "ua/cn/stu/navigation/MainActivity"));
    if (bleserv1 == NULL) return JNI_ERR;
    send_method = (*env)->GetStaticMethodID(env, bleserv, "send_to_ble","([B)I");
    if (send_method == NULL) return JNI_ERR;
    status_upd_method = (*env)->GetStaticMethodID(env, bleserv, "upd_status_param","(I[B[B)V");
    if (status_upd_method == NULL) return JNI_ERR;

    TaskCreate(status_thread, &thr);
    eth_router_init();
    eth_ble_if_init();
    ble_tcp_init();

    MX_LWIP_Init();
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_ua_cn_stu_navigation_MainActivity_char_1wr_1cbk(JNIEnv *env, jobject thiz, jint status) {
    ble_tx_complete(status);
}

JNIEXPORT void JNICALL Java_ua_cn_stu_navigation_MainActivity_new_1dg_1from_1bt(JNIEnv *env, jobject thiz, jbyteArray jdgram) {
    jbyte *stateJava = (*env)->GetByteArrayElements(env, jdgram, NULL);
    if (stateJava == NULL) return ;
    jint len = (*env)->GetArrayLength(env, jdgram);
    if (len>0) process_gatt_data(stateJava, len);
//    int pid=getpid();
//    int ppid=getppid();
//    int tid = gettid();
//    print_log("tid %d pid %d ppid %d recvd %d bytes",tid, pid, ppid, len);
    (*env)->ReleaseByteArrayElements(env, jdgram, stateJava, 0);
}

JNIEXPORT void JNICALL Java_ua_cn_stu_navigation_MainActivity_eth_1ble_1stack_1control(JNIEnv *env, jobject thiz, jint status) {
// status:
// 0 - service started
// 1 - service shutdown request
// 2 - ble connection up
// 3 - ble connection down
  ble_connection_status=status;
  if (status==2) gatt_conn_reset();
}
