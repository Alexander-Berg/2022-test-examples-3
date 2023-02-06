-- Fake blackbox DB queries

--name: update_user
UPDATE fbb.users
    set userinfo_response = :userinfo_response
WHERE uid = :uid
