#pragma once

#include "lwip/opt.h"
#include "lwip/mem.h"
#include "lwip/memp.h"
#include "netif/etharp.h"
#include "lwip/netif.h"
#include "lwip/timeouts.h"

void MX_LWIP_Init(void);

extern struct netif gnetif;
extern struct netif bridgeif;
extern sem_t lwip_core_sem;

#define LOCK_LWIP_CORE() sem_wait(&lwip_core_sem)
#define UNLOCK_LWIP_CORE() sem_post(&lwip_core_sem)
