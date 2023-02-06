
-- name: set_worker_for_subscription
UPDATE mail.shared_folder_subscriptions
   SET worker_id = :worker_id
 WHERE uid = :uid
   AND subscription_id = :subscription_id

-- name: assign_all_free_subscriptions
UPDATE mail.shared_folder_subscriptions
   SET worker_id = :worker_id
 WHERE worker_id IS NULL
