
-- name: sharddb_max_uid
SELECT max(uid)
  FROM shards.users
 WHERE uid BETWEEN :min_uid AND :max_uid

-- name: fbbdb_max_uid
SELECT max(uid)
  FROM fbb.users
  WHERE uid BETWEEN :min_uid AND :max_uid

-- name: fbbdb_max_suid
SELECT max(suid)
  FROM fbb.users
  WHERE suid BETWEEN :min_suid AND :max_suid

-- name: sharddb_max_shard_id
SELECT max(shard_id)
  FROM shards.shards
