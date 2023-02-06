-- name: clear_storage_delete_queue
DELETE FROM mail.storage_delete_queue

-- name: fill_storage_delete_queue
INSERT INTO mail.storage_delete_queue (uid, st_id)
     SELECT :uid, st_id
       FROM unnest(:stids) as st_id
