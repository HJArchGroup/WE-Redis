/*
 * Copyright (c) 2015, Feng Xie <fengxie at hotmail dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of Redis nor the names of its contributors may be used
 *     to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

 #include "redis.h"
 #include "adlist.h"

/*-----------------------------------------------------------------------------
 * Lock Commands
 *----------------------------------------------------------------------------*/

int isActiveClient(uint64_t id) {
    listNode *p = server.clients->head;
    while (p != NULL) {
        if (id == ((redisClient *)(p->value))->id) {
            return REDIS_OK;
        }
        p = p->next;
    }
    return REDIS_ERR;
}

void lockCommand(redisClient *c) {
    trylockCommand(c);
}

void broadcast(robj *o) {
    listNode *p = server.clients->head;
    while (p != NULL) {
        addReply((redisClient *)(p->value),o);
        p = p->next;
    }
}

void trylockCommand(redisClient *c) {
    robj *o = lookupKey(c->db,c->argv[1]);

    if (o == NULL) { // The mutex does not exist, then initiate it.
        o = createObject(REDIS_LOCK,c);
        setKey(c->db,c->argv[1],o);
        addReply(c,shared.ok);
        return;
    }

    if (o->type != REDIS_LOCK) { // The key is not a mutex.
        addReply(c,shared.wrongtypeerr);
        return;
    }

    if (o->ptr == NULL) { // The mutex is unlocked.
        o->ptr = c;
        addReply(c,shared.ok);
        return;
    }

    if (((redisClient *)(o->ptr))->id == c->id) {
        addReply(c,shared.reentryerr);
        return;
    }

    // The mutex had been locked by an active client already.
    if (isActiveClient(((redisClient *)(o->ptr))->id) == REDIS_OK) {
        addReply(c,shared.err);
        return;
    }

    o->ptr = c;
    addReply(c,shared.ok);
}

void unlockCommand(redisClient *c) {
    robj *o = lookupKey(c->db,c->argv[1]);

    if (o == NULL) { // The mutex does not exist.
        addReply(c,shared.nolockerr);
        return;
    }

    if (o->type != REDIS_LOCK) { // The key is not a mutex.
        addReply(c,shared.wrongtypeerr);
        return;
    }

    if (o->ptr == NULL) { // The mutex is unlocked.
        addReply(c,shared.err);
        return;
    }

    // The mutex had been locked by another client before.
    if (((redisClient *)(o->ptr))->id != c->id) {
        addReply(c,shared.err);
        return;
    }

    o->ptr = NULL;
    addReply(c,shared.ok);
}

void lockStatusCommand(redisClient *c) {
    robj *o = lookupKey(c->db,c->argv[1]);

    if (o == NULL) { // The mutex does not exist.
        addReply(c,shared.nolockerr);
        return;
    }

    if (o->type != REDIS_LOCK) { // The key is not a mutex.
        addReply(c,shared.wrongtypeerr);
        return;
    }

    if (o->ptr == NULL) {
        addReply(c,shared.unlocked);
        return;
    }

    if (isActiveClient(((redisClient *)(o->ptr))->id) == REDIS_OK) {
        addReply(c,shared.locked);
    }
    else {
        o->ptr = NULL;
        addReply(c,shared.unlocked);
    }
}
