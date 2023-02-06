-- name: clear_tabs
DELETE FROM mail.tabs
      WHERE uid = :uid

-- name: unvisit_tab
UPDATE mail.tabs
   SET unvisited = true
 WHERE uid = :uid
   AND tab = :tab

-- name: set_tab_fresh
UPDATE mail.tabs
   SET fresh_count = :count
 WHERE uid = :uid
   AND tab = :tab
