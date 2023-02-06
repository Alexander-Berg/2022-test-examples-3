-- Husky sharddb queries

-- name: get_user_shard_id
SELECT shard_id
  FROM shards.users
 WHERE uid = :uid

-- name: get_deleted_user_shard_id
SELECT shard_id
  FROM shards.deleted_users
 WHERE uid = :uid

-- name: add_user_to_stoplist
INSERT INTO transfer.stoplist (uid)
  VALUES (:uid)

-- name: get_user
SELECT *
  FROM mail.users
 WHERE uid = :uid

-- name: set_user_state
UPDATE mail.users
   SET state = :state,
       notifies_count = :notifies_count
 WHERE uid = :uid

-- name: set_user_archivation_state
INSERT INTO mail.archives(uid, state)
VALUES (:uid, :state)
