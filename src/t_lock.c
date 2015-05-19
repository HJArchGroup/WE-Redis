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

void setLockValue(redisClient *c, int value) {
    if (value == 1) {
        setKey(c->db,c->argv[1],shared.locked);
    }
    else if (value == 0) {
        setKey(c->db,c->argv[1],shared.unlocked);
    }
    addReply(c,shared.ok);
    //notifyKeyspaceEvent(REDIS_NOTIFY_STRING,"set",c->argv[1],c->db->id);
    //server.dirty++;
}

int isActiveClient(uint64_t id) {
    listNode *p = server.clients->head;
    while (p != NULL) {
        if (id == ((redisClient *)(p->value))->id) {
            return 1;
        }
        p = p->next;
    }
    return 0;
}

void lockCommand(redisClient *c) {
    robj *o = lookupKey(c->db,c->argv[1]);
    if (o == NULL) {
        // The mutex does not exist, then initiate it.
        setLockValue(c,1);
    }
    else {
        if (o->type == REDIS_LOCK) {
            if (o == shared.unlocked) {
                // The mutex is unlocked.
                setLockValue(c,1);
            }
            else {
                // The mutex is locked.
                addReply(c,shared.err);
            }
        }
        else {
            // The key is not a mutex.
            addReply(c,shared.wrongtypeerr);
        }
    }
}

void trylockCommand(redisClient *c) {
    robj *o = lookupKey(c->db,c->argv[1]);
    if (o == NULL) {
        // The mutex does not exist, then initiate it.
        setLockValue(c,1);
    }
    else {
        if (o->type == REDIS_LOCK) {
            if (o == shared.unlocked) {
                // The mutex is unlocked.
                setLockValue(c,1);
            }
            else {
                // The mutex is locked.
                addReply(c,shared.err);
            }
        }
        else {
            // The key is not a mutex.
            addReply(c,shared.wrongtypeerr);
        }
    }
}

void unlockCommand(redisClient *c) {
    robj *o = lookupKey(c->db,c->argv[1]);
    if (o == NULL) {
        // The mutex does not exist.
        addReply(c,shared.nolockerr);
    }
    else {
        if (o->type == REDIS_LOCK) {
            if (o == shared.unlocked) {
                // The mutex is unlocked.
                addReply(c,shared.err);
            }
            else {
                // Unlock the mutex straightly.
                setLockValue(c,0);
            }
        }
        else {
            // The key is not a mutex.
            addReply(c,shared.wrongtypeerr);
        }
    }
}

void lockStatusCommand(redisClient *c) {
    robj *o = lookupKey(c->db,c->argv[1]);
    if (o == NULL) {
        // The mutex does not exist.
        addReply(c,shared.nolockerr);
    }
    else {
        if (o->type == REDIS_LOCK) {
            if (o == shared.locked) {
                addReply(c,shared.lockedstr);
            }
            else {
                addReply(c,shared.unlockedstr);
            }
        }
        else {
            // The key is not a mutex.
            addReply(c,shared.wrongtypeerr);
        }
    }
}
