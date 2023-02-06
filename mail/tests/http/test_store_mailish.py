# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from dns import resolver

from hamcrest import assert_that

from lib.matchers.delivery import has_delivered_message_with_subject
from lib.msgs_builder import build_plain_text_message
from lib.psql import find_mail_label_rows_by_name, find_mailish_message_by_mid, find_fid_by_path, \
    find_mail_box_row_by_mid, find_mail_label_rows_by_fields, find_mail_label_rows_by_field
from lib.random_generator import get_random_string, get_random_number
from lib.users import get_user
from lib.utils import db_date_format_to_unix_timestamp


def resolve_ip(dns_name):
    dns_resolver = resolver.Resolver()
    try:
        result = dns_resolver.query(dns_name, "A")
    except resolver.NoAnswer:
        result = dns_resolver.query(dns_name, "AAAA")
    return result[0].address


def build_received_header(recv_from, by, rcpt_email):
    return """from {recv_from} ({recv_from} [{ip}])
 by {by} SMTPS id h68sor10550828qke.186.2019.10.15.08.12.13
 for {rcpt} (Google Transport Security);
 Tue, 15 Oct 2019 08:12:13 -0700 (PDT)""".format(recv_from=recv_from, ip=resolve_ip(recv_from), by=by, rcpt=rcpt_email)


def get_lid_of_label(uid, name, type=None):
    labels = find_mail_label_rows_by_field(uid, 'name', name)
    if labels and type is not None:
        labels = filter(lambda label: label.type == type, labels)
    if not labels or len(labels) != 1:
        msg = "is no" if not labels else "are more than one"
        raise RuntimeError("There {0} label with name={1} for user with uid={2}".format(msg, name, uid))
    return labels[0].lid


def get_store_mailish_json(to, received_date=1617949492, enable_push=False, lids=[], symbol=[]):
    return {
        "options": {
            "enable_push": enable_push
        },
        "mail_info": {
            "received_date": received_date,
            "labels": {
                "lids": lids,
                "symbol": symbol
            }
        },
        "user_info": {
            "email": to
        }
    }


pytestmark = [pytest.mark.bigmail, pytest.mark.qa, pytest.mark.mxbackout]


def test_store_mailish(http_client, sender, mailish_rcpt, external_imap_id):
    subject = get_random_string(25)
    msg = build_plain_text_message(sender.email, [mailish_rcpt.email], "Hello", {"Subject": subject})

    fid = find_fid_by_path(mailish_rcpt.uid, "Inbox")
    TIMESTAMP = 100500
    json = get_store_mailish_json(mailish_rcpt.email, received_date=TIMESTAMP)
    resp = http_client.store_mailish(json, msg.as_string(), mailish_rcpt.uid, external_imap_id, fid)
    assert resp.status_code == 200
    assert_that(mailish_rcpt, has_delivered_message_with_subject(subject))

    msg = find_mailish_message_by_mid(mailish_rcpt.uid, resp.json()["mid"])
    assert msg is not None
    assert msg.fid == fid
    assert msg.imap_id == external_imap_id
    assert TIMESTAMP == db_date_format_to_unix_timestamp(msg.imap_time)


def test_store_so_labels(http_client, mailish_rcpt, external_imap_id):
    rcpt = get_user("ExternalRcpt2")
    msg = build_plain_text_message("no-reply@accounts.google.com", [rcpt.email], "", {
        "Received": build_received_header("mail-sor-f73.google.com", "mx.google.com", rcpt.email),
        "Subject": get_random_string(25)
    })

    fid = find_fid_by_path(mailish_rcpt.uid, "Inbox")
    json = get_store_mailish_json(mailish_rcpt.email)
    resp = http_client.store_mailish(json, msg.as_string(), mailish_rcpt.uid, external_imap_id, fid)
    assert resp.status_code == 200

    msg = find_mail_box_row_by_mid(mailish_rcpt.uid, resp.json()["mid"])
    assert get_lid_of_label(mailish_rcpt.uid, "vtnrf0googlecom", "domain") in msg.lids

    trust_labels = find_mail_label_rows_by_fields(mailish_rcpt.uid, 'name', [str(idx) for idx in range(51, 57)])
    assert len(trust_labels) > 0
    assert any(label.lid in msg.lids for label in trust_labels)


def test_store_labels(http_client, sender, mailish_rcpt, external_imap_id):
    msg = build_plain_text_message(sender.email, [mailish_rcpt.email], "", {"Subject": get_random_string(25)})

    label_rows = find_mail_label_rows_by_name(mailish_rcpt.uid, "priority_high")
    assert len(label_rows) == 1
    priority_high_lid = label_rows[0].lid

    external_imap_id = get_random_number(6)
    fid = find_fid_by_path(mailish_rcpt.uid, "Inbox")
    json = get_store_mailish_json(mailish_rcpt.email, symbol=["seen_label", "deleted_label", "recent_label"] , lids=[str(priority_high_lid)])
    resp = http_client.store_mailish(json, msg.as_string(), mailish_rcpt.uid, external_imap_id, fid)
    assert resp.status_code == 200

    msg = find_mail_box_row_by_mid(mailish_rcpt.uid, resp.json()["mid"])
    assert priority_high_lid in msg.lids
    assert msg.seen is True
    assert msg.deleted is True
    assert msg.recent is True
