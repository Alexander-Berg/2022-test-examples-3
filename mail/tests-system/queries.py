DB_USER = "xeno"


def mailish_account(uid):
    return "SELECT * FROM mailish.accounts WHERE uid = {};".format(uid)


def mailish_auth_data(uid):
    return "SELECT * FROM mailish.auth_data WHERE uid = {}".format(uid)


def mailish_folders(uid):
    return "SELECT * FROM mailish.folders WHERE uid = {}".format(uid)


def mailish_folder_by_imap_path(uid, imap_path):
    return "SELECT * FROM mailish.folders WHERE uid = {} AND imap_path = '{}'".format(
        uid, imap_path
    )


def mailish_message_by_mid(uid, mid):
    return "SELECT * FROM mailish.messages WHERE uid = {} AND mid = {};".format(uid, mid)


def mailish_message_by_imap_id(uid, fid, imap_id):
    return "SELECT * FROM mailish.messages WHERE uid = {} AND fid = {} AND imap_id = {};".format(
        uid, fid, imap_id
    )


def mailish_messages_by_fid(uid, fid):
    return "SELECT * FROM mailish.messages WHERE uid = {} AND fid = {};".format(uid, fid)


def messages(uid):
    return """
        SELECT
            mb.mid as mid, mb.fid as fid, mm.hdr_message_id as msg_id, mb.tab as tab
        FROM
            mail.box mb
        JOIN
            mail.messages mm
        ON
            mb.uid = mm.uid AND mb.mid = mm.mid
        WHERE
            mb.uid = {};""".format(
        uid
    )


def messages_by_fid(uid, fid):
    return """
        SELECT
            mid, fid
        FROM
            mail.box
        WHERE
            uid = {} AND fid = {};""".format(
        uid, fid
    )


def messages_mids_by_seen_status(uid, fid, seen):
    return "SELECT mid FROM mail.box WHERE uid = {} AND fid = {} AND seen={};".format(
        uid, fid, seen
    )


def labels(uid, mid):
    return """
        SELECT
            lid, name, type
        FROM
            mail.labels
        WHERE
            uid = {} AND lid IN (
                SELECT
                    unnest(lids) AS lid
                FROM
                    mail.box
                WHERE
                    mid = {}
            );""".format(
        uid, mid
    )


def label_by_name(uid, label):
    return "SELECT * FROM mail.labels WHERE uid = {} AND name = '{}'".format(uid, label)


def folder_by_fid(uid, fid):
    return "SELECT * FROM mail.folders WHERE uid = {} AND fid = '{}'".format(uid, fid)


def changes(uid, change_type, from_time):
    return "SELECT changed::text, quiet FROM mail.change_log WHERE uid = {} AND type = '{}' AND change_date > '{}'".format(
        uid, change_type, from_time
    )


def set_user_is_here(uid, value):
    return "UPDATE mail.users SET is_here = {} WHERE uid = {}".format(
        "true" if value else "false", uid
    )


def set_here_since_to_current_time(uid):
    return "UPDATE mail.users SET here_since = now() WHERE uid = {}".format(uid)


def delete_mailish_account(uid):
    return "DELETE FROM mailish.accounts WHERE uid = {};".format(uid)


def delete_auth_data(uid):
    return "DELETE FROM mailish.auth_data WHERE uid = {};".format(uid)


def delete_auth_data_by_token(uid, token_id):
    return "DELETE FROM mailish.auth_data WHERE uid = {} AND token_id = '{}';".format(uid, token_id)


def delete_mailish_folders(uid):
    return "DELETE FROM mailish.folders WHERE uid = {};".format(uid)


def delete_user_folders(uid):
    return "DELETE FROM mail.folders WHERE uid = {} AND fid > 6;".format(uid)


def delete_labels(uid):
    return "DELETE FROM mail.labels WHERE uid = {} AND name NOT IN ('draft');".format(uid)


def delete_messages(uid, mids):
    return "SELECT * FROM code.delete_messages({},'{}');".format(uid, "{" + ", ".join(mids) + "}")


def update_security_lock(uid, security_lock):
    return "UPDATE mailish.auth_data SET lock_flag = {} WHERE uid = {}".format(security_lock, uid)


def mark_message_as_error(uid, mid):
    return "UPDATE mailish.messages SET mid = NULL, errors = 1 WHERE uid = {} AND mid = {}".format(
        uid, mid
    )
