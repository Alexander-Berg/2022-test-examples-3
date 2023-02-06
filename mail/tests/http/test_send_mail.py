# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from hamcrest import assert_that, has_item, has_items, has_entry, \
    has_length, has_property, all_of

from lib.fids import INBOX_FID, SENT_FID
from lib.matchers.delivery import has_delivered_message_with_subject, \
    has_delivered_messages_with_subject
from lib.psql import find_mail_message_row_by_subject, \
    find_box_rows_by_subject, find_mail_label_rows_by_name, \
    find_several_mail_label_rows
from lib.random_generator import get_random_string
from lib.nwsmtp.http_client import HTTPClient
from lib.users import get_user, get_users
from lib.msgs_builder import build_plain_text_message, STRONG_SPAM_TEXT, URL_RBL, \
    RFC_FAIL, BAD_KARMA, MAIL_LIMITS, PDD_ADMIN_KARMA, SPAM_COMPL, BOUNCES


def has_item_with_fid(fid, deleted=False):
    if deleted:
        return has_item(has_property("info", has_entry("fid", fid)))
    return has_item(has_property("fid", fid))


@pytest.mark.mxbackout
@pytest.mark.parametrize("save_to_sent, rcpts, fids, deleted", [
    ("0", ["DefaultRcpt", "DefaultSender"], [INBOX_FID], False),
    ("0", ["DefaultRcpt"], [], True),
    ("1", ["DefaultRcpt", "DefaultSender"], [SENT_FID, INBOX_FID], False),
    ("1", ["DefaultRcpt"], [SENT_FID], False)
])
def test_send_mail_save_to_sent(save_to_sent, rcpts, fids, deleted):
    sender, recipient = get_users("DefaultSender", "DefaultRcpt")
    subject = get_random_string(25)

    to = [get_user(name).email for name in rcpts]

    msg = build_plain_text_message(
        sender.email,
        to,
        "Hello",
        {"Subject": subject}
    )

    resp = HTTPClient().send_mail(
        {"to": to, "save_to_sent": save_to_sent},
        msg,
        uid=sender.uid,
        email=sender.email,
    )

    assert resp.status_code == 200
    assert_that(recipient, has_delivered_message_with_subject(subject))
    recepient_mail_box_rows = find_box_rows_by_subject(recipient.uid, subject)
    assert_that(recepient_mail_box_rows, all_of(
        has_length(1),
        has_item_with_fid(INBOX_FID)
    ))

    deliver_to_sender = len(fids) + (1 if deleted else 0)
    assert_that(sender, has_delivered_messages_with_subject(subject, deliver_to_sender))

    sender_mail_box_rows = find_box_rows_by_subject(sender.uid, subject)
    for fid in fids:
        assert_that(sender_mail_box_rows, has_item_with_fid(fid))

    if deleted:
        sender_deleted_box_rows = find_box_rows_by_subject(sender.uid, subject, deleted=True)
        assert sender_deleted_box_rows


@pytest.mark.mxbackout
def test_msg_headers_stored_to_db_in_correct_format():
    sender, recipient = get_users("DefaultSender", "DefaultRcpt")
    subject = get_random_string(25)
    text = get_random_string(5)

    msg = build_plain_text_message(
        sender.email,
        [recipient.email],
        text,
        {"Subject": subject}
    )

    resp = HTTPClient().send_mail(
        {"to": [recipient.email]},
        msg,
        uid=sender.uid,
        email=sender.email,
    )

    assert resp.status_code == 200
    assert_that(recipient, has_delivered_message_with_subject(subject))

    row = find_mail_message_row_by_subject(sender.uid, subject)

    assert_that(row, all_of(
        has_property("subject", subject),
        has_property("firstline", text)
    ))

    assert_that(row.recipients, all_of(
        has_length(3),
        has_item(all_of(
            has_entry("type", "from"),
            has_entry("email", sender.email)
        )),
        has_item(all_of(
            has_entry("type", "reply-to"),
            has_entry("email", sender.email)
        )),
        has_item(all_of(
            has_entry("type", "to"),
            has_entry("email", recipient.email)
        ))
    ))


@pytest.mark.mxbackout
def test_add_lids_to_sender():
    """
    Тест на добавление меток отправителю через ручку нв
      Ищем существующие у юзера метки в базе.
      Добавляем их лиды в запрос
      Проверяем что лида появились у письма в базе
    """
    sender, recipient = get_users("DefaultSender", "DefaultRcpt")
    subject = get_random_string(25)

    msg = build_plain_text_message(
        sender.email,
        [sender.email, recipient.email],
        "Hello",
        {"Subject": subject}
    )

    label_rows = find_several_mail_label_rows(sender.uid, 1)
    assert_that(label_rows, has_length(1))
    lid = label_rows[0].lid

    resp = HTTPClient().send_mail(
        {"to": [sender.email, recipient.email], "lid": [str(lid)]},
        msg,
        uid=sender.uid,
        email=sender.email,
    )

    assert resp.status_code == 200
    assert_that(sender, has_delivered_message_with_subject(subject))

    delivered_lids = find_box_rows_by_subject(sender.uid, subject)[0].lids
    assert lid in delivered_lids


@pytest.mark.mxbackout
@pytest.mark.parametrize("sender_labels, common_labels", [
    ([], []),
    (["sender_label1", "sender_label2"], []),
    (["sender_label"], ["recipient_label"]),
    ([], ["recipient_label1", "recipient_label2"])
])
def test_adding_sender_and_common_labels(sender_labels, common_labels):
    sender, recipient = get_users("DefaultSender", "DefaultRcpt")
    subject = get_random_string(25)

    msg = build_plain_text_message(
        sender.email,
        [sender.email, recipient.email],
        "Hello",
        {"Subject": subject}
    )

    resp = HTTPClient().send_mail(
        {
            "to": [sender.email, recipient.email],
            "sender_label": sender_labels,
            "common_label": common_labels
        },
        msg,
        uid=sender.uid,
        email=sender.email,
    )

    assert resp.status_code == 200

    assert_that(sender, has_delivered_message_with_subject(subject))
    assert_that(recipient, has_delivered_message_with_subject(subject))

    delivered_sender_lids = find_box_rows_by_subject(sender.uid, subject)[0].lids
    expected_sender_lids = [find_mail_label_rows_by_name(sender.uid, label)[0].lid
                            for label in sender_labels + common_labels]
    assert_that(delivered_sender_lids, has_items(*expected_sender_lids))

    delivered_recipient_lids = find_box_rows_by_subject(recipient.uid, subject)[0].lids
    expected_recipient_lids = [find_mail_label_rows_by_name(recipient.uid, label)[0].lid
                               for label in common_labels]
    assert_that(delivered_recipient_lids, has_items(*expected_recipient_lids))


@pytest.mark.mxbackout
@pytest.mark.parametrize("gtube, ban_reason", [
    (URL_RBL, "UrlRbl"),
    (RFC_FAIL, "RfcFail"),
    (BAD_KARMA, "BadKarma"),
    (MAIL_LIMITS, "MailLimits"),
    (PDD_ADMIN_KARMA, "PddAdminKarma"),
    (SPAM_COMPL, "SpamCompl"),
    (BOUNCES, "Bounces"),
], ids=["url_rbl", "rfc_fail", "bad_karma", "mail_limits", "pdd_admin_karma", "spam_compl", "bounces"])
def test_ban_reason(gtube, ban_reason):
    sender, recipient = get_users("DefaultSender", "DefaultRcpt")
    subject = get_random_string(25)

    msg = build_plain_text_message(
        sender.email,
        [recipient.email],
        STRONG_SPAM_TEXT + "\r\n" + gtube,
        {"Subject": subject}
    )

    resp = HTTPClient().send_mail(
        {"to": [recipient.email], "lid": []},
        msg,
        uid=sender.uid,
        email=sender.email,
        detect_spam=True,
    )

    assert resp.status_code == 406
    assert resp.json()["error"] == "StrongSpam"
    assert resp.json()["ban_reason"] == ban_reason
