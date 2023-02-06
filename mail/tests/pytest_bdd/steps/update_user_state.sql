-- name: get_user_state
SELECT state
  FROM mail.users
 WHERE uid = :uid

-- name: set_user_state
UPDATE mail.users
   SET state = :new_state
 WHERE uid = :uid
