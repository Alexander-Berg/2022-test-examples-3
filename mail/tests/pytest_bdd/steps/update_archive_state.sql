-- name: get_archive_state
SELECT state
  FROM mail.archives
WHERE uid = :uid

-- name: get_archive_notice
SELECT notice
  FROM mail.archives
WHERE uid = :uid

-- name: set_archive_state
INSERT INTO
    mail.archives (uid, state)
VALUES
    (:uid, :state)
ON CONFLICT (uid) DO UPDATE SET
    state = excluded.state

-- name: set_archive_notice
INSERT INTO
    mail.archives (uid, notice)
VALUES
    (:uid, :notice)
ON CONFLICT (uid) DO UPDATE SET
    notice = excluded.notice
