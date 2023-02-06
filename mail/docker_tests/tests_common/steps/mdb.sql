-- Mdb steps queries

-- name: set_inbox_create
UPDATE mail.folders
  SET created = :cr
 WHERE uid = :uid and type = 'inbox'

-- name: set_received_date
UPDATE mail.box
   SET received_date = :received_date
 WHERE uid = :uid
   AND mid = :mid
