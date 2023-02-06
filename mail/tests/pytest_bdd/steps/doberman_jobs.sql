-- name: unset_all_workers
UPDATE mail.shared_folder_subscriptions
   SET worker_id = NULL
 WHERE worker_id IS NOT NULL

-- name: delete_all_jobs
DELETE FROM mail.doberman_jobs
