# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from lib.fids import INBOX_FID, SENT_FID
from lib.psql import find_mail_box_row_by_mid, find_fid_by_path, find_mail_label_rows_by_fields, \
    find_mail_label_rows_by_name, find_mail_message_row_by_mid
from lib.utils import db_date_format_to_unix_timestamp


def get_store_json(to, received_date=1617949492, symbol=[], enable_push=False, folder_path=""):
    return {
        "options": {
            "detect_spam": False,
            "detect_virus": False,
            "detect_loop": False,
            "enable_push": enable_push,
            "allow_duplicates": False,
            "use_filters": False
        },
        "user_info": {
            "email": to
        },
        "mail_info": {
            "folder_path": folder_path,
            "received_date": received_date,
            "labels": {
                "system": [],
                "imap": [],
                "symbol": symbol,
                "lids": []
            }
        }
    }


pytestmark = [pytest.mark.bigmail, pytest.mark.qa, pytest.mark.mxbackout]


def test_store(http_client, sender, rcpt, rfc822_part):
    TIMESTAMP = 100500
    fid = find_fid_by_path(rcpt.uid, "Inbox")
    json = get_store_json(rcpt.email, received_date=TIMESTAMP)
    resp = http_client.store(json, rfc822_part, rcpt.uid, fid)
    assert resp.status_code == 200

    row = find_mail_box_row_by_mid(rcpt.uid, resp.json()["mid"])
    assert row is not None
    assert row.fid == fid, "Message for {0} should be delivered to fid={1}".format(rcpt.email, fid)
    assert TIMESTAMP == db_date_format_to_unix_timestamp(row.received_date)


@pytest.mark.parametrize("fid, allow_duplicates", [
    (INBOX_FID, True),
    (SENT_FID, False),
])
def test_allow_duplicates(http_client, rcpt, rfc822_part, fid, allow_duplicates):
    """ By now 'allow_duplicates' option does not have any effect due to MAILDLV-4658
        Deduplication rules for service=imap are triggered by imap=1 hint.
    """
    date = "Date: Sun, 10 Oct 2020 01:00:00 +0300\r\n"
    msg_id = "Message-Id: 123\r\n"
    subject = "Subject: Hello\r\n"
    rfc822_part = date + msg_id + subject + rfc822_part

    json = get_store_json(rcpt.email)
    json["options"]["allow_duplicates"] = allow_duplicates

    mids = []
    for _ in xrange(2):
        resp = http_client.store(json, rfc822_part, rcpt.uid, fid=fid, service="imap")
        assert resp.status_code == 200
        mids.append(resp.json()["mid"])

    if allow_duplicates:
        assert mids[0] != mids[1]
    else:
        assert mids[0] == mids[1]


def test_filters_applied(http_client, rcpt, rfc822_part):
    fid = find_fid_by_path(rcpt.uid, "Inbox")
    rfc822_part = "Subject: Red_Label\r\n" + rfc822_part

    json = get_store_json(rcpt.email)
    json["options"]["use_filters"] = True
    resp = http_client.store(json, rfc822_part, uid=rcpt.uid, fid=fid, service="collectors")
    assert resp.status_code == 200

    label_rows = find_mail_label_rows_by_name(rcpt.uid, "red")
    assert len(label_rows) == 1
    red_label_id = label_rows[0].lid

    row = find_mail_box_row_by_mid(rcpt.uid, resp.json()["mid"])
    assert red_label_id in row.lids


def test_store_labels(http_client, rcpt, rfc822_part):
    fid = find_fid_by_path(rcpt.uid, "Inbox")
    label_rows = find_mail_label_rows_by_name(rcpt.uid, "priority_high")
    assert len(label_rows) == 1
    priority_high_lid = label_rows[0].lid

    json = get_store_json(rcpt.email)
    json["mail_info"]["labels"] = {
        "lids": [str(priority_high_lid)],
        "imap": ["RED"],
        "symbol": ["seen_label", "answered_label", "draft_label"],
        "user": ["user_label"]
    }

    resp = http_client.store(json, rfc822_part, rcpt.uid, service="imap", fid=fid)
    assert resp.status_code == 200
    mid = resp.json()["mid"]

    labels = find_mail_label_rows_by_fields(rcpt.uid, 'name', ['answered', 'draft', "RED", "user_label"])

    mail_box_row = find_mail_box_row_by_mid(rcpt.uid, mid)

    assert len(labels) > 0
    assert set([label.lid for label in labels]).issubset(mail_box_row.lids)
    assert priority_high_lid in mail_box_row.lids
    assert mail_box_row.seen is True


def test_message_from_imap_has_append_attribute(http_client, rcpt, rfc822_part):
    fid = find_fid_by_path(rcpt.uid, "Inbox")
    json = get_store_json(rcpt.email)
    resp = http_client.store(json, rfc822_part, rcpt.uid, service="imap", fid=fid)
    assert resp.status_code == 200
    mid = resp.json()["mid"]

    message_row = find_mail_message_row_by_mid(rcpt.uid, mid)
    assert "append" in message_row.attributes


def test_store_by_folder_path(http_client, sender, rcpt, rfc822_part):
    folder_path = "Inbox"
    fid = find_fid_by_path(rcpt.uid, folder_path)
    json = get_store_json(rcpt.email, folder_path=folder_path)
    resp = http_client.store(json, rfc822_part, rcpt.uid)
    assert resp.status_code == 200

    row = find_mail_box_row_by_mid(rcpt.uid, resp.json()["mid"])
    assert row is not None
    assert row.fid == fid, "Message for {0} should be delivered to folder_path={1}".format(rcpt.email, folder_path)
