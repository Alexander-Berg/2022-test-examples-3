# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from email.utils import make_msgid
from hamcrest import assert_that

from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.msgs_builder import build_message_from_file
from lib.psql import find_mail_message_row_by_hdr_message_id, find_mail_message_row
from lib.smtp_send_tools import send_letter, send_plain_text_message
from lib.users import get_user, get_users
from lib.utils import get_letter_path, get_description

ASSERT_MSG = "Неверная запись в mail.messages, отличается поле: {}"


@pytest.mark.mxback
@pytest.mark.yaback
@pytest.mark.mxfront
@pytest.mark.parametrize("eml_name, expected", get_description("pq").items())
def test_mail_messages_table_fields(eml_name, expected):
    rcpt = get_user("TestMailMessageTable")

    fd = open(get_letter_path("pq", eml_name))

    msg_id = make_msgid()
    msg = build_message_from_file(fd, rcpt.email, {"Message-Id": msg_id})

    sender = get_user("DefaultSender")
    send_letter(sender, rcpt.email, msg)
    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))

    row = find_mail_message_row_by_hdr_message_id(rcpt.uid, msg_id)

    assert row.firstline == expected["firstline"], ASSERT_MSG.format("firstline")

    actual = {i["type"]: i for i in row.recipients}
    for exp in expected["recipients"]:
        if exp["type"] == "to":
            exp["email"] = rcpt.email
        assert actual[exp["type"]] == exp, ASSERT_MSG.format("recipients")

    assert (not row.attaches and not expected["attaches"]) or sorted(row.attaches) == sorted(
        expected["attaches"]), ASSERT_MSG.format("attaches")


@pytest.mark.qa
@pytest.mark.corp
@pytest.mark.mxfront
def test_bad_reply_to_value():
    """ Test that bad reply-to value is written to xdb in the wrong way.
    It happens because postfix replaces value with "Ostrovok.ru, newsletter@ostrovok.ru" (no quotes).
    The test is intended to fix the moment when postfix will be removed from cluster
     so behaviour would change.

     See MPROTO-4429 for additional info
    """
    sender, rcpt = get_users("DefaultSender", "DefaultRcpt")
    headers = {Header.REPLY_TO: """"Ostrovok.ru" newsletter@ostrovok.ru"""}
    msg_id = send_plain_text_message(sender, rcpt.email, headers=headers)
    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))

    row = find_mail_message_row(rcpt.uid, hdr_message_id=msg_id)
    reply_to = next(i for i in row.recipients if i["type"] == "reply-to")
    assert reply_to["email"] == "", "Expected bad email value, see MPROTO-4429"
    assert reply_to["name"] == "Ostrovok.ru,newsletter@ostrovok.ru", "Expected bad display name, see MPROTO-4429"
