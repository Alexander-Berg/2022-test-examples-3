-- name: add_to_storage_delete_queue
INSERT INTO mail.storage_delete_queue
    (uid, st_id, deleted_date)
VALUES
    (:uid, :st_id, :deleted_date)
