#!/usr/bin/env python

from nose.tools import assert_equals, assert_not_equal, assert_regexp_matches, assert_in, assert_not_in, assert_list_equal, assert_true

import os
import sys
import psycopg2
import datetime
import shards
from time import sleep

TEST_REPEAT_NEW_MESSAGES = 5
MAX_LOCAL_ID = 0x7FFFFFFFFFFFFFFF

def store_add(uid, gid, service, ttl, data, hash, transit_id, topic = None):
    topic_arg = " , '" + str(topic) + "'" if topic is not None else ''
    return shards.from_gid(gid).run(
      "select * from code.add_notification('" + str(uid)
        + "','" + str(service)
        + "', now()"
        + " , NULL, '', " + str(ttl)
        + " ,'" + str(data) + "'::bytea"
        + " , '" + str(hash) + "'"
        + " , '" + str(transit_id) + "'"
        + topic_arg
      + ");")

def store_list(uid, gid, service, hours, limit):
    return shards.from_gid(gid).run(
      "select * from code.list_notifications_gid('" + str(uid)
        + "','" + str(service)
        + "','" + str(hours)
        + "','" + str(limit)
      + "');")

def store_list_by_ids(uid, gid, service, start, end, limit, begin_ts, end_ts):
    store_sql = """
      select * from code.list_notifications_by_ids(%s,%s,%s,%s,%s,%s,%s);
    """
    return shards.from_gid(gid).run(store_sql, uid, service, start, end,
        limit, begin_ts, end_ts)

def store_get_counters(uid, gid, service):
    return shards.from_gid(gid).run(
      "select * from code.get_counters('" + str(uid)
       + "','{" + str(service)
      + "}');")

def store_get_range_counters(uid, gid, service, begin_ts):
    return shards.from_gid(gid).run(
      "select * from code.get_range_counters('" + str(uid)
       + "','{" + str(service)
      + "}', '" + begin_ts + "'::timestamptz);")

def store_lift_messages(uid, gid, service, local_id_from, count):
    return shards.from_gid(gid).run(
      "select * from code.lift_messages('" + str(uid)
        + "','" + str(service)
        + "', " + str(local_id_from)
        + " , " + str(count)
      + ");")

def setup():
    shards.setup()

def teardown():
    shards.teardown()

def test_counters(): # checks that procedure is executable
    rows = store_get_counters('1', 1, 'fake')

def test_range_counters(): # checks that procedure is executable
    rows = store_get_range_counters('1', 1, 'fake',
        str(datetime.datetime.now()-datetime.timedelta(hours = 1)))
    assert_equals(len(rows), 1)

def get_next_local_id(uid, gid, service):
    rows = store_get_counters(uid, gid, service)
    if len(rows) == 0:
        return 0
    return rows[0][4]

def test_add():
    before = len(store_list_by_ids('1', 1, 'fake', 0, MAX_LOCAL_ID, 1000, None, None))
    store_add('1', 1, 'fake', 1, 'testdata', str(datetime.datetime.now()), 'transit1')
    after = len(store_list_by_ids('1', 1, 'fake', 0, MAX_LOCAL_ID, 1000, None, None))
    assert_equals(before+1, after)

def test_duplicates():
    datahash = datetime.datetime.now()
    id1 = store_add('1', 1, 'fake', 1, 'testdata', str(datahash), 'transit1')[0][0]
    id2 = store_add('1', 1, 'fake', 1, 'anothertestdata', str(datahash), 'transit2')[0][0]
    assert_equals(id1, id2)

def test_list():
    local_id = store_add('1', 1, 'fake', 1, 'testdata', str(datetime.datetime.now()), 'transit1')[0][0]

    messages = store_list_by_ids('1', 1, 'fake', local_id, MAX_LOCAL_ID, 1000, None, None)
    assert_equals(len(messages), 1)

    now = datetime.datetime.now()
    yesterday = now - datetime.timedelta(days=1)
    messages = store_list_by_ids('1', 1, 'fake', local_id, MAX_LOCAL_ID, 1000, yesterday, now)
    assert_equals(len(messages), 1)

    before_yesterday = yesterday - datetime.timedelta(days=1)
    messages = store_list_by_ids('1', 1, 'fake', local_id, MAX_LOCAL_ID, 1000, before_yesterday, yesterday)
    assert_equals(len(messages), 0)

def test_add_topic():
    local_id = int(get_next_local_id('1', 1, 'fake'))
    store_add('1', 1, 'fake', 1, 'testdata', str(datetime.datetime.now()), 'transit1', 'topic')
    last_inserted = store_list_by_ids('1', 1, 'fake', local_id, MAX_LOCAL_ID, 1000, None, None)
    assert_equals(len(last_inserted), 1)
    assert_equals(last_inserted[0][10], 'topic')

def test_add_update_by_topic():
    local_id = int(get_next_local_id('1', 1, 'fake'))
    store_add('1', 1, 'fake', 1, 'testdata', str(datetime.datetime.now()), 'transit1', 'topic')
    store_add('1', 1, 'fake', 1, 'testdata2', str(datetime.datetime.now()), 'transit2')
    store_add('1', 1, 'fake', 1, 'testdata3', str(datetime.datetime.now()), 'transit3')
    store_add('1', 1, 'fake', 1, 'testdata4', str(datetime.datetime.now()), 'transit4', 'topic2')
    topic_result = store_add('1', 1, 'fake', 10, 'testdata5', str(datetime.datetime.now()), 'transit5', 'topic')

    assert_equals(topic_result[0][1], 'transit1') # returns transit id we merged with
    assert_equals(topic_result[0][4], 'topic')

    messages = store_list_by_ids('1', 1, 'fake', local_id, MAX_LOCAL_ID, 1000, None, None)
    assert_equals(len(messages), 4)
    assert_true(messages[0][10] is None)
    assert_equals(str(messages[0][6]), 'testdata2')
    assert_true(messages[1][10] is None)
    assert_equals(str(messages[1][6]), 'testdata3')
    assert_equals(messages[2][10], 'topic2')
    assert_equals(str(messages[2][6]), 'testdata4')
    assert_equals(messages[3][10], 'topic')
    assert_equals(str(messages[3][6]), 'testdata5')
    assert_equals(messages[3][5], 1) # updating topic must not increase message ttl

def test_repeat_message():
    # Add TEST_REPEAT_NEW_MESSAGES messages.
    messages = [('message_{}'.format(i), '{}_{}'.format(datetime.datetime.now(), i)) for i in xrange(TEST_REPEAT_NEW_MESSAGES)]
    ids = [store_add('1', 1, 'fake', 10, message[0], message[1], 'transit1')[0][0] for message in messages]

    repeat_count = TEST_REPEAT_NEW_MESSAGES - 2;
    local_id_from = ids[-repeat_count-1];

    # Repeat messages between first and last.
    res = store_lift_messages('1', 1, 'fake', local_id_from, repeat_count)
    assert_equals(res[0][0], repeat_count)

    repeated_messages = store_list_by_ids('1', 1, 'fake', local_id_from, MAX_LOCAL_ID, 1000, None, None)[-repeat_count:]
    assert_equals(ids[-1] + 1, repeated_messages[0][2])
    for i in xrange(repeat_count):
        assert_equals(messages[i+1][0], repeated_messages[i][6][:])

    # Only one additional message can be repeated.
    res = store_lift_messages('1', 1, 'fake', local_id_from, repeat_count + 1)
    assert_equals(res[0][0], 1)
    repeated = store_list_by_ids('1', 1, 'fake', local_id_from, MAX_LOCAL_ID, 1000, None, None)[-1]
    assert_equals(repeated_messages[-1][2] + 1, repeated[2])
    assert_equals(messages[-1][0], repeated[6][:])

def test_repeat_message_nonexisting():
    local_id = store_add('1', 1, 'fake', 10, 'single message', str(datetime.datetime.now()), 'transit1')[0][0]

    # Try to repeate more messages than exists.
    res = store_lift_messages('1', 1, 'fake', local_id, 3)
    assert_equals(res[0][0], 1)

    repeated = store_list_by_ids('1', 1, 'fake', local_id, MAX_LOCAL_ID, 1000, None, None)[-1]
    assert_equals(local_id + 1, repeated[2])
    assert_equals('single message', repeated[6][:])

def test_repeat_message_ttl():
    local_id = store_add('1', 1, 'fake', 10, 'message1', str(datetime.datetime.now()) + '1', 'transit1')[0][0]
    store_add('1', 1, 'fake', 5, 'message2', str(datetime.datetime.now()) + '2', 'transit2')
    store_add('1', 1, 'fake', 10, 'message3', str(datetime.datetime.now()) + '3', 'transit3')
    sleep(1)

    # Try to repeat all messages. Message with small remaining ttl must be skipped.
    res = store_lift_messages('1', 1, 'fake', local_id, 3)
    assert_equals(res[0][0], 2)
    repeated_messages = store_list_by_ids('1', 1, 'fake', local_id, MAX_LOCAL_ID, 1000, None, None)[-2:]
    assert_equals(repeated_messages[0][6][:], 'message1')
    assert_equals(repeated_messages[1][6][:], 'message3')
