#!/usr/bin/env python

import os
import sys
import psycopg2
import datetime

def table_add(uid, gid, service, subid, callback, filter, extra_data, client, ttl, session):
    get_shard(gid).run("select * from code.add_subscription('" + str(uid)
      + "','" + str(gid)
      + "','" + str(service)
      + "','" + str(subid)
      + "','" + str(callback)
      + "','" + str(filter)
      + "','" + str(extra_data)
      + "','" + str(client)
      + "'," + str(ttl)
      + ",'" + str(session)
    + "');")

class Shard:
  def __init__(self, start_gid, end_gid, conninfo):
    self.start_gid = start_gid
    self.end_gid = end_gid
    self.connection = psycopg2.connect(conninfo)
    self.connection.set_isolation_level(0)
    self.cursor = self.connection.cursor()

  def run(self, sql):
    print sql
    self.cursor.execute(sql)

  def matches(self, gid):
    return self.start_gid <= gid and gid <= self.end_gid

  def close(self):
    self.connection.close()

global shards

def get_shard(gid):
  return filter(lambda shard: shard.matches(gid), shards)[0]

shards = [
  Shard(0, 32768, os.environ["SHARD_1"]),
  Shard(32769, 65535, os.environ["SHARD_2"])
]

# Cleanup subs from previous tests. Manually delete all subscriptions
# with service 'mesh-autotest', since the set of subscriptions generated
# may change between mesh versions
for shard in shards:
  shard.run("delete from xiva.subscriptions where service='mesh-autotest'")

# Add subscriptions for tests, distributing them between 2 shards.
# If there is only one shard, this still works, but it does not
# matter, whether subs are added at the beginning or at the end of
# the whole gid range
def create_subscriptions(users, gids):
  for i in users:
      for j in gids:
          table_add(i, j, 'mesh-autotest', 'id'+':'+str(i)+':'+str(j),
              'http://push-sandbox.yandex.ru/ping', '', '', 'test', 1, 'ABC')

MAX_GID=65536
create_subscriptions((1, 2), range(1,6))
create_subscriptions((MAX_GID-1, MAX_GID-2), range(MAX_GID-5, MAX_GID))

table_add(3, 11, 'mesh-autotest', 'id:3:11',
    'http://push-sandbox.yandex.ru/ping',
    '{"vars":{},"rules":[{"if":{"$has_tags":["tagA"]},"do":"skip"},{"do":"send_bright"}]}',
    '', 'test', 1, 'ABC')

for shard in shards:
  shard.close()
