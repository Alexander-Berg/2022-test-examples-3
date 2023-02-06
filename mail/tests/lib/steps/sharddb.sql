-- name: open_shard_registartion
UPDATE shards.shards
   SET reg_weight = 100
 WHERE shard_id = :shard_id;

-- name: get_shard_registartion
SELECT *
  FROM shards.shards
 WHERE shard_id = :shard_id
   AND reg_weight > 0

-- name: move_user_to_deleted
WITH deleted_users AS (
    DELETE FROM shards.users
     WHERE uid = :uid
    RETURNING uid, shard_id, data
)
INSERT INTO shards.deleted_users
    (uid, shard_id, data)
SELECT uid, shard_id, data
  FROM deleted_users
ON CONFLICT DO NOTHING

-- name: move_deleted_user_to_users
WITH deleted_users AS (
    DELETE FROM shards.deleted_users
     WHERE uid = :uid
    RETURNING uid, shard_id, data
)
INSERT INTO shards.users
    (uid, shard_id, data)
SELECT uid, shard_id, data
  FROM deleted_users
ON CONFLICT DO NOTHING

-- name: drop_user_from_deleted_users
DELETE FROM shards.deleted_users
 WHERE uid = :uid

-- name: drop_user_from_users
DELETE FROM shards.users
 WHERE uid = :uid

-- name: change_deleted_user_shard
UPDATE shards.deleted_users
   SET shard_id = :shard_id
 WHERE uid = :uid

-- name: change_user_shard
UPDATE shards.users
   SET shard_id = :shard_id
 WHERE uid = :uid

-- name: get_user
SELECT uid
  FROM shards.users
 WHERE uid = :uid

-- name: get_deleted_user
SELECT uid
  FROM shards.deleted_users
 WHERE uid = :uid

-- name: get_user_shard
SELECT shard_id
  FROM shards.users
 WHERE uid = :uid
