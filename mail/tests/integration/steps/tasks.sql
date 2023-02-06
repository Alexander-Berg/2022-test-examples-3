
-- name: get_task
SELECT transfer_id, uid, task, task_args,
       status, error_type, try_notices, tries, last_update
  FROM transfer.users_in_dogsleds
 WHERE transfer_id = :transfer_id
UNION
SELECT transfer_id, uid, task, task_args,
       status, error_type, try_notices, tries, last_update
  FROM transfer.processed_tasks
 WHERE transfer_id = :transfer_id
