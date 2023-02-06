-- name: clear_unsubscribe_tasks
DELETE FROM mail.unsubscribe_tasks

-- name: assign_unsubscribe_task
UPDATE mail.unsubscribe_tasks
SET assigned = current_date + :passed
WHERE task_id = :task_id
