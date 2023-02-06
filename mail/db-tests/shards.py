import psycopg2
import psycopg2.extras
import os

class Shard:
  def __init__(self, id, start_gid, end_gid, conninfo):
    self.id = id
    self.start_gid = start_gid
    self.end_gid = end_gid
    self.connection = psycopg2.connect(conninfo)
    self.connection.autocommit = True
    self.cursor = self.connection.cursor(cursor_factory=psycopg2.extras.DictCursor)

  def run(self, sql, *args):
    bound_sql = self.cursor.mogrify(sql, args)
    self.cursor.execute(bound_sql)
    return self.cursor.fetchall()

  def matches(self, gid):
    return self.start_gid <= gid and gid <= self.end_gid

  def close(self):
    self.connection.close()

get_shards_sql = """
  with mapping as (
      select part_id, start_key, end_key
      from plproxy.key_ranges
    ), conninfos as (
      select shard_id, role, conn_string
      from plproxy.get_partitions()
    )
  select part_id, start_key, end_key, conn_string
  from mapping, conninfos
  where mapping.part_id = conninfos.shard_id
    and conninfos.role = 'master';
"""

def from_gid(gid):
  return filter(lambda shard: shard.matches(gid), shards)[0]

def from_id(id):
  return filter(lambda shard: shard.id == id, shards)[0]

shards = []

def setup_fixed_shard():
  global shards
  conninfo = os.environ["FIXED_SHARD"]
  shards.append(Shard(0, 0, 65535, conninfo))

def setup_plproxy_shards():
  global shards
  plproxy_conninfo = os.environ["TEST_CONNINFO"]
  conninfo_params = os.environ["TEST_CONNINFO_PARAMS"]

  # Get current set of shards from plproxy
  connection = psycopg2.connect(plproxy_conninfo)
  connection.set_isolation_level(0)
  cursor = connection.cursor()
  cursor.execute(get_shards_sql)
  rows = cursor.fetchall()
  shards = [Shard(row[0], row[1], row[2], row[3] + ' ' + conninfo_params) for row in rows]

def setup():
  if "FIXED_SHARD" in os.environ:
    setup_fixed_shard()
  else:
    setup_plproxy_shards()

def teardown():
  for shard in shards:
      shard.close()
