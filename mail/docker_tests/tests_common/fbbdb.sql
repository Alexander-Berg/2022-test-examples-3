--name: add_user
INSERT INTO fbb.users
    (uid, suid, login, db)
VALUES
    (:uid, :suid, :login, :db)

--name: is_user_exists
SELECT 1 FROM fbb.users
 WHERE uid = :uid

--name: remove_user
DELETE FROM fbb.users
 WHERE uid = :uid
    OR suid = :suid
    OR login = :login

--name: update_user_sids
UPDATE fbb.users
   SET sids = :sids
 WHERE uid = :uid
    OR suid = :suid
    OR login = :login

--name: add_corp_mailing_list
INSERT INTO fbb.users
    (uid, suid, login, db, is_corp, is_maillist)
VALUES
    (:uid, :suid, :login, :db, true, true)
