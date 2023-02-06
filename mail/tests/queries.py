# coding: utf-8


def message(uid, mid):
    return """
        SELECT
            mm.st_id,
            mm.size,
            mm.subject,
            mm.firstline,
            mm.hdr_date,
            mm.hdr_message_id,
            mm.attributes,
            mb.fid,
            mb.tid,
            mb.received_date,
            mb.lids
        FROM
            mail.messages mm
        JOIN
            mail.box mb
        ON
            mm.uid = mb.uid AND mm.mid = mb.mid
        WHERE
            mm.uid = {} AND mm.mid = {}""".format(uid, mid)


def label_by_name(uid, name, type):
    return "SELECT * FROM mail.labels WHERE uid = {} AND name = '{}' AND type = '{}'".format(uid, name, type)


def folder_by_type(uid, type):
    return "SELECT * FROM mail.folders WHERE uid = {} AND type = '{}'".format(uid, type)


def recipients(uid, mid):
    return """
        SELECT
            (recipient).*
        FROM (
            SELECT
                UNNEST(recipients) as recipient
            FROM
                mail.messages
            WHERE
                uid = {} AND
                mid = {}
        ) AS r
        ORDER BY
            type, email, name""".format(uid, mid)


def mime_parts(uid, mid):
    return """
        SELECT
            (mime).*
        FROM (
            SELECT
                UNNEST(mime) as mime
            FROM
                mail.messages
            WHERE
                uid = {} AND
                mid = {}
        ) AS m
        ORDER BY
            hid""".format(uid, mid)


def attachments(uid, mid):
    return """
        SELECT
            (attachment).*
        FROM (
            SELECT
                UNNEST(attaches) as attachment
            FROM
                mail.messages
            WHERE
                uid = {} AND
                mid = {}
        ) AS a
        ORDER BY
            hid""".format(uid, mid)


def attributes(uid, mid):
    return """
        SELECT
            UNNEST(attributes) as attributes
        FROM
            mail.messages
        WHERE
            uid = {} AND
            mid = {}""".format(uid, mid)


def message_tab(uid, mid):
    return "SELECT tab FROM mail.box WHERE uid = {} AND mid = '{}'".format(uid, mid)
