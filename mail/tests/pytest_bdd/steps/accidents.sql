
-- name: accidently_delete_label
DELETE FROM mail.labels
WHERE uid = :uid
  AND lid = :lid

-- name: accidently_delete_folder
DELETE FROM mail.folders
 WHERE uid = :uid
   AND fid = :fid

-- name: accidently_set_box_chain
UPDATE mail.box
   SET chain = :chain
 WHERE uid = :uid
   AND mid = :mid

-- name: accidently_insert_into_pop3_box
INSERT INTO mail.pop3_box
    (uid, mid, fid, size)
VALUES
    (:uid, :mid, :fid, :size)

-- name: accidently_set_doom_date
UPDATE mail.box
   SET doom_date = :doom_date
 WHERE uid = :uid
   AND mid = :mid

-- name: accidentally_delete_from_threads
DELETE FROM mail.threads
 WHERE uid = :uid
   AND tid = :tid

-- name: accidentally_add_thread
INSERT INTO mail.threads
  (uid, tid, revision,
  newest_mid, newest_date,
  message_count, message_seen)
VALUES
  (:uid, :tid, :revision,
  :newest_mid, :newest_date,
  :message_count, :message_seen)

-- name: accidentally_set_tid_and_newest_tif
UPDATE mail.box
   SET tid = :tid,
       newest_tif = :newest_tif
 WHERE uid = :uid
   AND mid = :mid

-- name: accidentally_set_newest_tit_and_newest_tif
UPDATE mail.box
   SET newest_tif = :newest_tif,
       newest_tit = :newest_tit
 WHERE uid = :uid
   AND mid = :mid
