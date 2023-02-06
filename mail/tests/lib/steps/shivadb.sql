-- Shiva shards queries
-- name: add_shiva_shard
INSERT INTO shiva.shards
    (shard_id, cluster_id, disk_size, used_size, shard_type, load_type, can_transfer_to, shard_name, migration, priority)
VALUES
    (:shard_id, :cluster_id, :disk_size, :used_size, :shard_type, :load_type, :can_transfer_to, :shard_name, :migration, :priority)

-- name: clear_shiva_shard_tasks
DELETE FROM shiva.shard_running_tasks

-- name: clear_shiva_shards
DELETE FROM shiva.shards

-- name: get_shiva_shard
SELECT shard_id, cluster_id, disk_size, used_size, shard_type, load_type, can_transfer_to, shard_name, migration, priority
FROM shiva.shards
WHERE shard_id = :shard_id

-- name: get_all_shiva_shards
SELECT shard_id, cluster_id, disk_size, used_size, shard_type, load_type, can_transfer_to, shard_name, migration, priority
FROM shiva.shards

-- name: get_shiva_tasks
SELECT *
FROM shiva.shard_running_tasks

-- name: get_transfer_tasks
SELECT task_args
FROM transfer.users_in_dogsleds
WHERE uid = :uid
