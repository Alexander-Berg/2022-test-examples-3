
-- name: reset_worker
SELECT code.reset_job(:worker_id)

-- name: subscription_exists
SELECT 1
  FROM mail.shared_folder_subscriptions
 WHERE uid = :uid
   AND subscription_id = :subscription_id
