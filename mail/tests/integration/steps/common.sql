-- name: add_task
INSERT INTO transfer.users_in_dogsleds
    (uid, task, task_args, shard_id, status)
VALUES
    (:uid, :task, :task_args, :shard_id, :status)
RETURNING transfer_id