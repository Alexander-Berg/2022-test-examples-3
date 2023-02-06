-- name: st_id_exists
SELECT EXISTS (
    SELECT 1
      FROM mail.storage_delete_queue
     WHERE st_id = :st_id)
    OR EXISTS (
    SELECT 1
      FROM mail.messages AS m
     WHERE hashtext(m.st_id) = hashtext(:st_id)
       AND m.st_id = :st_id)
