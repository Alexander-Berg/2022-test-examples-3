
-- name: assign_subscription
UPDATE mail.shared_folder_subscriptions
   SET worker_id = :worker_id
 WHERE uid = :uid
   AND subscription_id = :subscription_id

-- name: assign_subscription_and_set_state
UPDATE mail.shared_folder_subscriptions
   SET state = :state,
       worker_id = :worker_id
 WHERE uid = :uid
   AND subscription_id = :subscription_id

-- name: clear_other_doberman_jobs
DELETE
  FROM mail.doberman_jobs
 WHERE worker_id IS DISTINCT FROM :worker_id
   AND NOT code.is_system_worker(worker_id)

-- name: reset_worker_id
INSERT INTO mail.doberman_jobs
    (worker_id, launch_id, hostname,
     worker_version, assigned, heartbeated, timeout)
VALUES
    (:worker_id, null, null, null, null, null, null)
    ON CONFLICT (worker_id) DO UPDATE
SET
    launch_id = null,
    hostname = null,
    worker_version = null,
    assigned = null,
    heartbeated = null,
    timeout = null
RETURNING *, txid_current()

--name: get_worker_id
SELECT worker_id, launch_id, hostname,
       worker_version, assigned, heartbeated
  FROM mail.doberman_jobs
 WHERE worker_id = :worker_id

-- name: reset_subscription_state_to_init
UPDATE mail.shared_folder_subscriptions
   SET state = 'init'
 WHERE uid = :uid
   AND subscription_id = :subscription_id
