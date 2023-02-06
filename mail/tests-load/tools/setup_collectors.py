#!/usr/bin/env python

import sys, psycopg2, threading, base64
from multiprocessing.dummy import Pool

WORKERS = 32
CONNINFO = "dbname='maildb' user='mxback' host='pgload03f.mail.yandex.net' port='6432' password='KphXRP3zqZhL394Zvff9ZrvWn'"

TARGET_OFFSET = 1000000
ACTIVE_RANGE = (10000001100001, 10000001200000)
INACTIVE_RANGE = (10000001200001, 10000001300000)

mutex = threading.Lock()
local = threading.local()


def get_conn():
    if not hasattr(local, "conn"):
        local.conn = psycopg2.connect(CONNINFO)
    return local.conn


def delete_collector(uids):
    cur = get_conn().cursor()
    cur.execute("""DELETE FROM mail.collectors WHERE uid = ANY(%s)""", (uids,))
    get_conn().commit()
    mutex.acquire()
    print "deleted", cur.rowcount, "collectors from", uids
    mutex.release()
    cur.close()


def create_collector(uids):
    cur = get_conn().cursor()
    sources = [uid - TARGET_OFFSET for uid in uids]
    tokens = [base64.urlsafe_b64encode(str(uid).encode()) for uid in uids]
    cur.execute(
        """SELECT code.create_collector(x.dst, x.src, x.token) FROM (SELECT * FROM unnest(%s,%s,%s) as t(dst,src,token)) x;""",
        (uids, sources, tokens),
    )
    get_conn().commit()
    mutex.acquire()
    print "created collector from", uids
    mutex.release()
    cur.close()


def clear_messages(uids):
    cur = get_conn().cursor()
    has_messages = True
    while has_messages:
        cur.execute(
            """SELECT code.purge_threads(x.uid), code.purge_box(x.uid), code.purge_messages(x.uid) FROM (SELECT * FROM unnest(%s) as t(uid)) x;""",
            (uids,),
        )
        has_messages = not cur.fetchone()[0] or not cur.fetchone()[1] or not cur.fetchone()[2]
        get_conn().commit()
    mutex.acquire()
    print "cleared messages from", uids
    mutex.release()
    cur.close()


def execute(func, uids):
    uids = [uid for uid in uids]
    print "execute", func.__name__, "for", len(uids), "uids"
    pool = Pool(processes=WORKERS)
    try:
        chunk_size = 1000
        pool.map(func, [uids[i : i + chunk_size] for i in xrange(0, len(uids), chunk_size)])
    except KeyboardInterrupt:
        pool.terminate()
        pool.join()


def setup_collectors(count, src_range):
    max_count = src_range[-1] - src_range[0] + 1
    if count > max_count or count < 0:
        raise Exception(
            "Can't create {} inactive collectors, max prepared accounts {}".format(count, max_count)
        )

    conn = psycopg2.connect(CONNINFO)
    cur = conn.cursor()

    target_range = (src_range[0] + TARGET_OFFSET, src_range[-1] + TARGET_OFFSET)
    cur.execute("SELECT uid from mail.collectors WHERE uid >= %s AND uid <= %s", target_range)
    current_collectors = {x[0] for x in cur.fetchall()}
    target_collectors = {x for x in range(target_range[0], target_range[0] + count)}

    execute(delete_collector, current_collectors - target_collectors)
    execute(create_collector, target_collectors - current_collectors)


def clear_range(count, src_range):
    max_count = src_range[-1] - src_range[0] + 1
    if count > max_count or count < 0:
        raise Exception(
            "Can't clear messages in {} accounts, max prepared accounts {}".format(count, max_count)
        )

    execute(clear_messages, [x for x in range(src_range[0], src_range[0] + count)])


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print ("Usage: {} INACTIVE_COLLECTORS_COUNT ACTIVE_COLLECTORS_COUNT".format(sys.argv[0]))
        exit(-1)

    print "setup inactive collectors:"
    inactive_count = int(sys.argv[1])
    setup_collectors(inactive_count, INACTIVE_RANGE)

    print "setup active collectors:"
    active_count = int(sys.argv[2])
    clear_range(active_count, ACTIVE_RANGE)
    setup_collectors(active_count, ACTIVE_RANGE)
