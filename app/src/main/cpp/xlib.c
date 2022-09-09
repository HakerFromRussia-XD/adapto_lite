#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <semaphore.h>
#include <stdatomic.h>
#include "xlib.h"

int dbg[11];

uint32_t xTaskGetTickCount() {
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return tv.tv_sec*1000L + tv.tv_usec/1000;
    }

void TNotifyCreate (struct NHandle_t* task) {
  atomic_init(&task->val, 0);
  sem_init(&task->sem,0,0);
  }

void TNotifyAdd (struct NHandle_t* task , uint32_t ulValue) {
    atomic_fetch_or(&task->val, ulValue);
    sem_post(&task->sem);
    }

uint32_t TNotifyWait(struct NHandle_t* task) {
  sem_wait(&task->sem);
  return atomic_exchange(&task->val, 0);
  }

void TaskCreate (void (*callback_fn)(void* arg), pthread_t *pxCreatedTask) {
  pthread_attr_t ta;
  pthread_attr_init(&ta);
//    struct sched_param sp={.sched_priority=sched_get_priority_max(SCHED_FIFO)};
//    pthread_attr_setschedparam(&ta, &sp);
//    pthread_attr_setschedpolicy(&ta, SCHED_FIFO);// SCHED_FIFO, SCHED_RR, SCHED_OTHER
  int tid1=pthread_create(pxCreatedTask, &ta, callback_fn, NULL);
  }

int print_log( const char * format, ... ) {
    uint8_t buf[5000];
    va_list args;
    va_start (args, format);
    vsprintf (buf,format, args);
    __android_log_write(ANDROID_LOG_ERROR, "C", buf);
    va_end (args);
    return 0;
}
