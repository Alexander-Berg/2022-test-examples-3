-- name: update_backup_state
UPDATE backup.backups
   SET state = :state
 WHERE uid = :uid
   AND backup_id = :backup_id

-- name: update_restore_state
UPDATE backup.restores
   SET state = :state
 WHERE uid = :uid
   AND backup_id = :backup_id
   AND created = :created
