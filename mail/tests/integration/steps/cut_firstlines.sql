-- name: get_new_messages
SELECT firstline, received_date
FROM mail.box JOIN mail.messages USING (uid, mid)
WHERE uid = :uid AND received_date > :border_date

-- name: get_old_messages
SELECT firstline, received_date
FROM mail.box JOIN mail.messages USING (uid, mid)
WHERE uid = :uid AND received_date <= :border_date

-- name: get_all_messages
SELECT firstline, received_date
FROM mail.box JOIN mail.messages USING (uid, mid)

