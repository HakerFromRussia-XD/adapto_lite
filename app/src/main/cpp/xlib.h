#pragma once

#define TaskHandle_t pthread_t
#include <stdatomic.h>
#include <semaphore.h>

extern int dbg[];

struct NHandle_t {
  atomic_int val;
  sem_t sem;
  };

uint32_t xTaskGetTickCount();
void TNotifyCreate(struct NHandle_t* tn);
void TNotifyAdd (struct NHandle_t* tn , uint32_t ulValue);
uint32_t TNotifyWait(struct NHandle_t* tn);

void TaskCreate (void (*TaskFn)(void* arg), pthread_t *pThr);
int print_log( const char * format, ... );
