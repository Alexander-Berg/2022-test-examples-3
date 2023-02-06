-- Shards queries
-- name: add_shard
INSERT INTO shards.shards
    (shard_id, name, load_type)
VALUES
    (:shard_id, :name, 'custom')

-- name: add_shard_instance
INSERT INTO shards.instances (instance_id, shard_id, host, port, dc, dbname)
SELECT max(instance_id) + 1, :shard_id, 'some_fake_instance', 6432, 'local', 'maildb'
  FROM shards.instances 

-- name: add_shard_to_shiva
INSERT INTO shiva.shards
    (shard_id, shard_name, can_transfer_to, load_type, cluster_id, migration, disk_size)
VALUES
    (:shard_id, :shard_name, :can_transfer_to, :load_type, :shard_name, 0, 111111111)

-- name: add_fake_husky_worker
 INSERT INTO buckets.buckets (bucket_id, worker, heartbeat, last_updated) 
      VALUES (:shard_id, 'fake_worker', now(), now()) 
 ON CONFLICT(bucket_id) DO UPDATE 
         SET heartbeat = now(), last_updated = now()

-- name: clean_husky_task_queue
DELETE FROM transfer.users_in_dogsleds

-- name: get_all_tasks
SELECT transfer_id, uid, task, task_args, status, shard_id, priority
  FROM transfer.users_in_dogsleds
