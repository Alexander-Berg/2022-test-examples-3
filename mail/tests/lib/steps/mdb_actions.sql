-- name: change_backup_state
UPDATE backup.backups
   SET state = :state
 WHERE uid = :uid
   AND backup_id = :backup_id

-- name: move_backup_updated_date
UPDATE backup.backups
   SET updated = updated - :days
 WHERE uid = :uid
   AND backup_id = :backup_id

-- name: move_doom_date
UPDATE mail.box
   SET doom_date = doom_date - :days
 WHERE uid = :uid
   AND mid = ANY(:mids)

-- name: move_deleted_date_in_deleted_box
UPDATE mail.deleted_box
   SET deleted_date = deleted_date - :days
 WHERE uid = :uid
   AND mid = ANY(:mids)

-- name: move_deleted_date_in_storage_delete_queue
UPDATE mail.storage_delete_queue
   SET deleted_date = deleted_date - :days
 WHERE uid = :uid
   AND st_id = ANY(:st_ids)

-- name: shift_purge_date
UPDATE mail.users
   SET purge_date=purge_date - :days
 WHERE uid = :uid

--name: move_chained_date
UPDATE mail.chained_log
   SET log_date = log_date - :days
 WHERE uid = :uid
   AND mid = ANY(:mids)

-- name: clear_storage_delete_queue
DELETE FROM mail.storage_delete_queue

--name: add_to_storage_delete_queue
SELECT code.add_to_storage_delete_queue(
    :uid, :st_id)

-- name: set_fails_count_in_storage_delete_queue
UPDATE mail.storage_delete_queue
   SET fails_count = :count
 WHERE uid = :uid
   AND st_id = ANY(:st_ids)

-- name: move_received_date
UPDATE mail.box
   SET received_date = now() - :days
 WHERE uid = :uid
   AND mid = ANY(:mids)

-- name: set_archivation_rule
SELECT code.set_folder_archivation_rule(
    :uid, :fid, :rule_type, :days, NULL, :max_size
)

-- name: make_shared_folder
SELECT code.add_folder_to_shared_folders(
    :uid, :fid
)

-- name: clean_users
UPDATE mail.users
   SET is_here = false

-- name: make_user_stats
INSERT INTO stats.users_info(uid, db_size, storage_size)
     VALUES(:uid, :db_size, :storage_size)

-- name: shift_stats_update_date
UPDATE stats.users_info
   SET last_update=last_update - :days
 WHERE uid = :uid

 -- name: get_fresh_stats_count
SELECT count(*)
  FROM stats.users_info
 WHERE uid = :uid
   AND last_update > now() - interval '1 hour'

 -- name: get_prepared_transaction_count
SELECT count(*)
  FROM pg_prepared_xacts

-- name: add_synced_attribute_to_messages
UPDATE mail.messages
   SET attributes = array_append(attributes, 'synced')
 WHERE uid = :uid

-- name: clean_user_change_log
DELETE FROM mail.change_log
 WHERE uid = :uid

 -- name: fill_user_change_log
INSERT INTO mail.change_log(uid, revision, type)
VALUES (:uid, 1, 'update')

-- name: set_user_state
UPDATE mail.users
   SET state = :state,
       last_state_update = now() - :days
 WHERE uid = :uid

-- name: set_user_archivation_state
INSERT INTO mail.archives(uid, state, updated)
VALUES (:uid, :state, now() - :days)

-- name: set_user_archive_message_count
UPDATE mail.archives
   SET message_count = :message_count
 WHERE uid = :uid

-- name: get_user_state
SELECT state
  FROM mail.users
 WHERE uid = :uid

-- name: get_user_archivation_state
SELECT state
  FROM mail.archives
 WHERE uid = :uid

-- name: get_user_archive_message_count
SELECT message_count
  FROM mail.archives
 WHERE uid = :uid

-- name: set_user_state_and_notifies_count
UPDATE mail.users
   SET state = :state,
       last_state_update = now() - :days,
       notifies_count = :notifies_count
 WHERE uid = :uid

-- name: get_user_notifies_count
SELECT notifies_count
  FROM mail.users
 WHERE uid = :uid

-- name: set_user_here_since
UPDATE mail.users
   SET here_since = :here_since::timestamptz
 WHERE uid = :uid

 -- name: get_user_is_here
SELECT is_here
  FROM mail.users
 WHERE uid = :uid
