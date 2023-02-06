-- name: set_user_state
UPDATE mail.users
   SET state = :state
 WHERE uid = :uid

-- name: set_user_archivation_state
INSERT INTO mail.archives(uid, state)
VALUES(:uid, :state)

 -- name: get_user_state
SELECT state
  FROM mail.users
 WHERE uid = :uid
