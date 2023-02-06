#!/usr/bin/env python

from nose.tools import assert_equals, assert_not_equal, assert_regexp_matches, assert_in, assert_not_in, assert_list_equal, assert_is_none, assert_is_not_none

import os
import sys
import psycopg2
import datetime
import shards
import time

def synchronous_commit(shard):
    return shard.run('SHOW synchronous_commit;')[0][0]

def table_add(uid, gid, service, subid, callback, filter, extra_data, client, ttl,
  session, init_local_id, platform, device, bb_conn_id, uidset, synchronous_commit=None):
    return shards.from_gid(gid).run(
      "select * from code.add_subscription('" + str(uid)
        + "','" + str(gid)
        + "','" + str(service)
        + "','" + str(subid)
        + "','" + str(callback)
        + "','" + str(filter)
        + "','" + str(extra_data)
        + "','" + str(client)
        + "'," + str(ttl)
        + ",'" + str(session)
        + "'," + str(init_local_id)
        + ",'" + str(platform)
        + "','" + str(device)
        + "','" + str(bb_conn_id)
        + "','" + str(uidset)
        + "',%s"
      + ");", synchronous_commit)

def table_del(uid, gid, service, subid, synchronous_commit=None):
    return shards.from_gid(gid).run(
      "select * from code.del_subscription('" + str(uid)
        + "','" + str(service)
        + "','" + str(subid)
        + "',0,%s"
      + ");", synchronous_commit)

def table_list(uid, gid, service):
    list_sql = """
      select
          id,
          callback,
          filter,
          extra_data,
          client,
          ttl,
          session_key,
          init_local_id,
          init_time,
          ack_local_id,
          ack_time,
          smart_notify,
          platform,
          device,
          retry_interval,
          next_retry_time,
          ack_event_ts
      from code.list_subscriptions
      where uid=%s and service=%s;
    """
    return shards.from_gid(gid).run(list_sql, uid, service)

def table_list_uidset(gid, service, uidset, select_list = '*'):
    return shards.from_gid(gid).run(
      'select ' + select_list +
      ' from code.list_subscriptions where service = %s and uidset = %s',
      service, uidset)

def table_update_ack(uid, gid, service, subid, old, new, smart, synchronous_commit=None):
    return table_update_ack_retry(uid, gid, service, subid, old, new, smart, None, None, synchronous_commit)

def table_update_ack_retry(uid, gid, service, subid, old, new, smart, retry_interval, ack_event_ts, synchronous_commit=None):
    sql = """
      select * from code.update_ack_retry(%s, %s, %s, %s, %s, %s, %s, %s, %s);
    """
    return shards.from_gid(gid).run(sql, uid, service, subid, old, new, smart, retry_interval, ack_event_ts, synchronous_commit)

def table_bulk_update_ack(gid, uid, service, subid, old, new, smart, synchronous_commit=None):
    return shards.from_gid(gid).run(
      "select code.bulk_update_ack('" + str(uid)
        + "','" + str(service)
        + "','" + str(subid)
        + "','" + str(old)
        + "','" + str(new)
        + "','" + str(smart)
        + "',%s"
      + ");", synchronous_commit)

def table_update_uidset(gid, uidset, service, old_cb, new_cb, synchronous_commit=None):
    old_cb_arg = "','" + str(old_cb) + "'" if old_cb is not None else "',NULL"
    return shards.from_gid(gid).run(
      "select code.update_subscriptions_uidset('" + str(uidset)
        + "','" + str(service)
        + old_cb_arg
        + ",'" + str(new_cb)
        + "',%s"
      + ");", synchronous_commit)

def table_del_all(uid, gid, service):
    rows = table_list(uid, gid, service)
    ids = map(lambda row: row['id'], rows)
    for subid in ids:
        table_del(uid, gid, service, subid)

def table_batch_del(uid, gid, service, ids, synchronous_commit=None):
  return shards.from_gid(gid).run(
    "select code.batch_del_subscriptions(%s,%s,%s,now(), %s)",
    uid, service, ids, synchronous_commit
  )

def setup():
    shards.setup()

def teardown():
    shards.teardown()

def make_sub(uid, service, id, callback='callback'):
  return (uid,0,service, id, callback, '{filter}', 'extra', 'test', 1, 'ABC-DEF-1',0,'fake-platform','fake-device-1', 't:123456', '')

class TestXtable():
  def setup(self):
      table_del_all('1', 1, 'fake')
      table_del_all('2', 1, 'fake')

  def test_add(self):
      table_add('1',1,'fake', 'id1', 'http://tst', '{filter}', 'extra', 'test', 1, 'ABC-DEF',0,'fake-platform','fake-device', 't:123456', '')
      ids = map(lambda row: row['id'], table_list('1',1,'fake'))
      assert_in('id1', ids)

  def test_del(self):
      table_add('1',1,'fake', 'id1', 'http://tst', '{filter}', 'extra', 'test', 1, 'ABC-DEF',0,'fake-platform','fake-device', 't:123456', '')
      table_del('1',1,'fake', 'id1')
      rows = table_list('1',1,'fake')
      assert_equals(len(rows), 0)

  def test_update_ack(self):
      table_add('1',1,'fake', 'id1', 'http://tst', '{filter}', 'extra', 'test', 1, 'ABC-DEF',0,'fake-platform','fake-device', 't:123456', '')
      time.sleep(1.5) # to ensure that ack_time and init_time are different
      assert_equals(table_update_ack('1',1,'fake', 'id1', 0, 4, False), [[4,]])
      rows = table_list('1',1,'fake')
      assert_equals(len(rows), 1)
      sub = rows[0]
      assert_not_equal(sub['init_time'], sub['ack_time']) # init_time != ack_time
      assert_is_none(sub['retry_interval'])
      assert_is_none(sub['next_retry_time'])

  def test_update_ack_interval_only(self):
      table_add('1',1,'fake', 'id1', 'http://tst', '{filter}', 'extra', 'test', 1, 'ABC-DEF',0,'fake-platform','fake-device', 't:123456', '')
      time.sleep(1.5) # to ensure that ack_time and init_time are different
      assert_equals(table_update_ack_retry('1',1,'fake', 'id1', 0, 0, False, datetime.timedelta(seconds=2), None), [[0,]])
      rows = table_list('1',1,'fake')
      assert_equals(len(rows), 1)
      sub = rows[0]
      assert_equals(sub['init_time'], sub['ack_time']) # init_time == ack_time
      assert_equals(sub['retry_interval'], 2)
      assert_is_not_none(sub['next_retry_time'])

      assert_equals(table_update_ack_retry('1',1,'fake', 'id1', 0, 0, False, None, None), [[0,]])
      rows = table_list('1',1,'fake')
      assert_equals(len(rows), 1)
      sub = rows[0]
      assert_equals(sub['init_time'], sub['ack_time']) # init_time == ack_time
      assert_is_none(sub['retry_interval'])
      assert_is_none(sub['next_retry_time'])

  def test_update_ack_event_ts(self):
      table_add('1',1,'fake', 'id1', 'http://tst', '{filter}', 'extra', 'test', 1, 'ABC-DEF',0,'fake-platform','fake-device', 't:123456', '')
      assert_equals(table_update_ack_retry('1',1,'fake', 'id1', 0, 1, False, None, datetime.date.today()), [[1,]])
      rows = table_list('1',1,'fake')
      assert_equals(len(rows), 1)
      sub = rows[0]
      assert_equals(datetime.date.fromtimestamp(sub['ack_event_ts']), datetime.date.today())

  def test_bulk_update_ack(self):
      table_add('1',1,'fake', 'id11', 'http://tst11', '{filter}', 'extra', 'test', 1, 'ABC-DEF-1',0,'fake-platform','fake-device-1', 't:123456', '')
      table_add('1',1,'fake', 'id12', 'http://tst12', '{filter}', 'extra', 'test', 1, 'ABC-DEF-2',0,'fake-platform','fake-device-2', 't:123457', '')
      table_add('2',1,'fake', 'id21', 'http://tst21', '{filter}', 'extra', 'test', 1, 'ABC-DEF-3',0,'fake-platform','fake-device-3', 't:123458', '')
      assert_equals(table_bulk_update_ack(1,'{"1","1","2"}','{"fake", "fake", "fake"}',
          '{"id11", "id12", "id21"}', '{0,0,0}', '{3,7,1}', '{f,f,f}'), [[3,],[7,],[1,]])

  def test_add_uidset(self):
      table_add('1',1,'fake', 'id1', 'http://tst', '{filter}', 'extra', 'test', 1, 'ABC-DEF',0,'fake-platform','fake-device', 't:123456', 'set1')
      ids = map(lambda row: row['id'], table_list_uidset(1,'fake','set1'))
      assert_in('id1', ids)

  def test_update_subscriptions_uidset(self):
      old_cb = 'http://old_cb'
      new_cb = 'http://new_cb'

      all_subs = [
        ('1',1,'fake', 'id11', old_cb, '{filter}', 'extra', 'test', 1, 'ABC-DEF-1',0,'fake-platform','fake-device-1', 't:123456', 'set1'),
        ('2',1,'fake', 'id21', old_cb, '{filter}', 'extra', 'test', 1, 'ABC-DEF-3',0,'fake-platform','fake-device-3', 't:123458', 'set1')
      ]

      for sub in all_subs:
        table_add(*sub)

      all_subs_updated = [
        ('1',1,'fake', 'id11', new_cb, '{filter}', 'extra', 'test', 1, 'ABC-DEF-1',0,'fake-platform','fake-device-1', 't:123456', 'set1'),
        ('2',1,'fake', 'id21', new_cb, '{filter}', 'extra', 'test', 1, 'ABC-DEF-3',0,'fake-platform','fake-device-3', 't:123458', 'set1')
      ]

      table_update_uidset(1, 'set1', 'fake', old_cb, new_cb)

      list_result = sorted(table_list_uidset(1, 'fake', 'set1', 'id, callback, filter'))
      assert_equals(list_result, [[s[3], s[4], s[5]] for s in all_subs_updated])

      table_update_uidset(1, 'set1', 'fake', 'http://wrong_cb', new_cb)

      # assert that subs still have new_cb for callback
      list_result = sorted(table_list_uidset(1, 'fake', 'set1', 'id, callback, filter'))
      assert_equals(list_result, [[s[3], s[4], s[5]] for s in all_subs_updated])

  def test_batch_update_subscriptions_no_callback(self):
      new_cb = 'http://new_cb'

      all_subs = [
        ('1',1,'fake', 'id11', 'http://tst11', '{filter}', 'extra', 'test', 1, 'ABC-DEF-1',0,'fake-platform','fake-device-1', 't:123456', 'set1'),
        ('2',1,'fake', 'id21', 'http://tst12', '{filter}', 'extra', 'test', 1, 'ABC-DEF-3',0,'fake-platform','fake-device-3', 't:123458', 'set1')
      ]

      for sub in all_subs:
        table_add(*sub)

      all_subs_updated = [
        ('1',1,'fake', 'id11', new_cb, '{filter}', 'extra', 'test', 1, 'ABC-DEF-1',0,'fake-platform','fake-device-1', 't:123456', 'set1'),
        ('2',1,'fake', 'id21', new_cb, '{filter}', 'extra', 'test', 1, 'ABC-DEF-3',0,'fake-platform','fake-device-3', 't:123458', 'set1')
      ]

      table_update_uidset(1, 'set1', 'fake', None, new_cb)

      list_result = sorted(table_list_uidset(1, 'fake', 'set1', 'id, callback, filter'))
      assert_equals(list_result, [[s[3], s[4], s[5]] for s in all_subs_updated])

  def test_batch_del_subscriptions(self):
      table_add(*make_sub(uid='uid_dies', service='service_dies', id='id_dies_1'))
      table_add(*make_sub(uid='uid_dies', service='service_dies', id='id_dies_2'))
      table_add(*make_sub(uid='uid_dies', service='service_dies', id='id_remains'))
      table_add(*make_sub(uid='uid_dies', service='service_remains', id='id_dies_1'))
      table_add(*make_sub(uid='uid_remains', service='service_dies', id='id_dies_2'))

      table_batch_del('uid_dies', 0, 'service_dies', ['id_dies_1', 'id_dies_2'])

      assert_equals('id_remains', table_list('uid_dies', 0, 'service_dies')[0]['id'])
      assert_equals('id_dies_1', table_list('uid_dies', 0, 'service_remains')[0]['id'])
      assert_equals('id_dies_2', table_list('uid_remains', 0, 'service_dies')[0]['id'])
      assert_equals(1, len(table_list('uid_dies', 0, 'service_dies')))

  def check_synchronous_commit(self, func, args):
      shard = shards.from_gid(1)
      shard.connection.autocommit = False

      try:
        # Assert that sychronous commit value is changed for the open transaction
        # and restored after transaction commits
        default_synchronous_commit = synchronous_commit(shard)
        desired_synchonous_commit = 'off' if default_synchronous_commit != 'off' else 'on'
        func(*args, synchronous_commit=desired_synchonous_commit)
        assert_equals(synchronous_commit(shard), desired_synchonous_commit)
        shard.connection.commit()
        assert_equals(synchronous_commit(shard), default_synchronous_commit)

      finally:
        shard.connection.rollback()
        shard.connection.autocommit = True


  def test_synchronous_commit_control(self):
      funcs_args = [
          (table_add, ('1',1,'fake', 'id11', 'http://tst11', '{filter}', 'extra',
           'test', 1, 'ABC-DEF-1',0,'fake-platform','fake-device-1', 't:123456',
           'uidset')),
          (table_del, ('1',1,'fake', 'id1')),
          (table_update_ack_retry, ('1',1,'fake', 'id1', 0, 0, False,
            datetime.timedelta(seconds=2), None)),
          (table_bulk_update_ack, (1,'{"1","1","2"}','{"fake", "fake", "fake"}',
            '{"id11", "id12", "id21"}', '{0,0,0}', '{3,7,1}', '{f,f,f}')),
          (table_update_uidset, (1, 'set1', 'fake', 'old_cb', 'new_cb')),
          (table_batch_del, ('1', 0, 'fake', ['id1', 'id2']))
      ]
      for f in funcs_args:
          yield self.check_synchronous_commit, f[0], f[1]
