#!/usr/bin/env python2.7

import json
import os
import random


TARGET_OFFSET = 1000000
DONOR_RANGE = (10000001000001, 10000001050000)
ACTIVE_RANGE = (10000001100001, 10000001200000)
INACTIVE_RANGE = (10000001200001, 10000001300000)


def prepare_bg_ammo(fname, settings):
    results = []
    for handler in settings.keys():
        for i in range(settings[handler]):
            ammo = {"tag": handler}
            results.append(ammo)
    random.shuffle(results)
    with open(fname, "w") as out:
        for i in results:
            out.write(json.dumps(i) + "\n")


def prepare_pg_ammo(fname, strart_src, count):
    # {"tag": "add_message", "donorUid": 10000001000001, "sourceUid": 10000001000001, "folderName": "Inbox", "folderType": "inbox"}
    with open(fname, "w") as out:
        results = []
        for i in xrange(count):
            src_uid = strart_src + i
            ammo = {
                "tag": "add_message",
                "sourceUid": src_uid,
                "donorUid": DONOR_RANGE[0],
                "folderName": "Inbox",
                "folderType": "inbox",
            }
            results.append(ammo)
        random.shuffle(results)
        for i in results:
            out.write(json.dumps(i) + "\n")


def prepare_create_ammo(fname, strart_src, start_dst, count):
    # {"tag": "create_collector", "sourceUid": 10000001000001, "targetUid": 10000002000001}
    with open(fname, "w") as out:
        for i in xrange(count):
            src_uid = strart_src + i
            dst_uid = start_dst + i
            ammo = {"tag": "create_collector", "sourceUid": src_uid, "targetUid": dst_uid}
            out.write(json.dumps(ammo) + "\n")
        for i in xrange(1000):
            ammo = {"tag": "check_create_schedule"}
            out.write(json.dumps(ammo) + "\n")


def prepare_list_ammo(fname, strart_srcs, counts):
    # {"tag": "list", "targetUid": 10000001000001}
    with open(fname, "w") as out:
        results = []
        for strart_src, count in zip(strart_srcs, counts):
            for i in xrange(count):
                src_uid = strart_src + i
                ammo = {"tag": "list", "targetUid": src_uid}
                results.append(ammo)
        random.shuffle(results)
        for i in results:
            out.write(json.dumps(i) + "\n")


if __name__ == "__main__":
    inactive_users_count = int(os.environ["INACTIVE_USERS_COUNT"])
    active_users_count = int(os.environ["ACTIVE_USERS_COUNT"])
    creates_count = int(os.environ["CREATES_COUNT"])
    prepare_bg_ammo("ping.ammo", {"ping": 10})
    prepare_pg_ammo("pg.ammo", ACTIVE_RANGE[0], active_users_count)
    prepare_create_ammo(
        "create.ammo", DONOR_RANGE[0], DONOR_RANGE[0] + TARGET_OFFSET, creates_count
    )
    prepare_list_ammo(
        "list.ammo",
        (ACTIVE_RANGE[0], INACTIVE_RANGE[0]),
        (active_users_count, inactive_users_count),
    )
