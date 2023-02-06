-- name: delete_all_tabs
DELETE
  FROM mail.tabs
 WHERE uid = :uid

-- name: get_can_read_tabs_setting
SELECT COALESCE(value->'single_settings'->>'can_read_tabs', '') AS value
  FROM settings.settings
 WHERE uid = :uid

-- name: set_can_read_tabs
UPDATE mail.users
   SET can_read_tabs = :value
 WHERE uid = :uid
