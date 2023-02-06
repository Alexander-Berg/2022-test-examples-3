-- Transfer queries
-- name: update_shard_id
UPDATE transfer.users_in_dogsleds
   SET shard_id = :shard_id
 WHERE transfer_id = :transfer_id

 -- name: delete_task
DELETE FROM transfer.users_in_dogsleds
 WHERE transfer_id = :transfer_id

 -- name: set_task_status_and_args
UPDATE transfer.users_in_dogsleds
   SET status = :status,
       task_args = :task_args
 WHERE transfer_id = :transfer_id

-- name: get_delayed_task
SELECT planned
  FROM transfer.users_in_dogsleds
 WHERE transfer_id = :transfer_id
   AND planned > now()
   AND status = 'pending'

-- name: get_dates_from_deleted_box
SELECT received_date
  FROM mail.deleted_box
 WHERE uid = :uid;
 
-- name: set_received_date_in_box
UPDATE mail.box
   SET received_date = :received_date 
 WHERE uid = :uid 
   AND mid in (
       SELECT mid
       FROM mail.box
      WHERE uid = :uid
      LIMIT :message_count);

  -- name: set_can_transfer_to
UPDATE shiva.shards set can_transfer_to = :can_transfer_to
 WHERE shard_id = :shard_id

-- name: get_task_in_users_in_dogsleds
SELECT transfer_id
  FROM transfer.users_in_dogsleds
 WHERE transfer_id = :transfer_id

-- name: get_task_in_processed_tasks
SELECT transfer_id
  FROM transfer.processed_tasks
 WHERE transfer_id = :transfer_id
