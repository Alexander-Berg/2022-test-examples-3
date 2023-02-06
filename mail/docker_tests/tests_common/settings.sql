-- name: create_settings_in_setdb
SELECT plproxy.create_settings(
    i_uid := :uid::bigint,
    i_value := :settings::json
) AS rows

-- name: get_settings_in_xdb
SELECT value::text FROM settings.settings
 WHERE uid = :uid

-- name: delete_settings_in_xdb
SELECT code.delete_settings(
    i_uid := :uid::bigint
) AS rows
