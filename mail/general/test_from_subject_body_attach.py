# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest
from hamcrest import assert_that

from email.utils import make_msgid
from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.msgs_builder import build_message_from_file
from lib.psql import find_mail_box_row_by_message_id
from lib.random_generator import get_random_string
from lib.smtp_send_tools import send_plain_text_message, send_letter
from lib.utils import get_letter_path
from lib.users import get_users


@pytest.mark.mxfront
@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.parametrize("user, subject, expected_fid", [
    ("UserWithFilterSubjectContainsQuery", "contains 16", 7),
    ("UserWithFilterSubjectNotContainsQuery", "not contains", 7),
    ("UserWithFilterSubjectMatchesQuery", "14", 7),
    ("UserWithFilterSubjectNotMatchesQuery", "not matches", 9)])
def test_rules_with_subject_queries(user, subject, expected_fid):
    # For UserWithFilterSubjectContainsQuery if subject contains "16" move to folder "16" with fid 7
    # For UserWithFilterSubjectNotContainsQuery if subject doesn't contains "письма фильтра 17" move to folder "17" with fid 7
    # For UserWithFilterSubjectMatchesQuery if subject matches "14" move to folder "16" with fid 7
    # For UserWithFilterSubjectNotMatchesQuery if subject doesn't matches "тема письма фильтра 15" move to folder "15" with fid 9

    sender, rcpt = get_users("DefaultSender", user)
    msg_id = send_plain_text_message(sender, rcpt.email, subject=subject)

    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    fid = find_mail_box_row_by_message_id(rcpt.uid, msg_id).fid
    assert fid == expected_fid, "Message for {0} should be delivered to fid {1}".format(rcpt.email, expected_fid)


@pytest.mark.bigmail
@pytest.mark.mxfront
@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.parametrize("user, hdr_from, expected_fid", [
    ("UserWithFilterFromContainsQuery", "<filter-02@yandex.ru>", 11),
    ("UserWithFilterFromNotContainsQuery", "mx-test-user@ya.ru", 15),
    ("UserWithFilterFromMatchesQuery", "filter-02@yandex.ru", 9),
    ("UserWithFilterFromNotMatchesQuery", "mx-test-user@ya.ru", 9)])
def test_rules_with_from_queries(user, hdr_from, expected_fid):
    # For UserWithFilterFromContainsQuery if From contains "filter-02@yandex.ru" move to folder "2" with fid 11
    # For UserWithFilterFromNotContainsQuery if From doesn't contains "filter-02@yandex.ru" (and filter-03 and filter-05) move to folder "5" with fid 15
    # For UserWithFilterFromMatchesQuery if From matches "filter-02@yandex.ru" move to folder "6" with fid 9
    # For UserWithFilterFromNotMatchesQuery if From doesn't matches "filter-02@yandex.ru" move to folder "3" with fid 9

    sender, rcpt = get_users("DefaultSender", user)
    msg_id = send_plain_text_message(sender, rcpt.email, headers={Header.FROM: hdr_from})

    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    fid = find_mail_box_row_by_message_id(rcpt.uid, msg_id).fid
    assert fid == expected_fid, "Message for {0} should be delivered to fid {1}".format(rcpt.email, expected_fid)


@pytest.mark.corp
@pytest.mark.mxfront
@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.parametrize("user, hdr_from, expected_fid", [
    ("UserWithFilterFromContainsQuery", "<filter-02@yandex.ru>", 11),
    ("UserWithFilterFromNotContainsQuery", "mx-test-user@ya.ru", 15),
    ("UserWithFilterFromMatchesQuery", "filter-02@yandex.ru", 9),
    ("UserWithFilterFromNotMatchesQuery", "mx-test-user@ya.ru", 9)])
def test_corp_rules_with_from_queries(user, hdr_from, expected_fid):
    # For UserWithFilterFromContainsQuery if From contains "filter-02@yandex.ru" move to folder "2" with fid 11
    # For UserWithFilterFromNotContainsQuery if From doesn't contains "filter-02@yandex.ru" (and filter-03 and filter-05) move to folder "5" with fid 15
    # For UserWithFilterFromMatchesQuery if From matches "filter-02@yandex.ru" move to folder "2" with fid 9
    # For UserWithFilterFromNotMatchesQuery if From doesn't matches "filter-02@yandex.ru" move to folder "3" with fid 9

    sender, rcpt = get_users("DefaultSender", user)
    msg_id = send_plain_text_message(sender, rcpt.email, headers={Header.FROM: hdr_from})

    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    fid = find_mail_box_row_by_message_id(rcpt.uid, msg_id).fid
    assert fid == expected_fid, "Message for {0} should be delivered to fid {1}".format(rcpt.email, expected_fid)


@pytest.mark.mxfront
@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.parametrize("user, text, expected_fid", [
    ("UserWithFilterMessageBodyContainsQuery", "my 20 messages", 9),
    ("UserWithFilterMessageBodyNotContainsQuery", "not contains", 11),
    ("UserWithFilterMessageBodyMatchesQuery", "тело письма фильтра 18", 9)], ids=["contains", "not_contains", "matches"])
def test_rules_with_message_body_queries(user, text, expected_fid):
    # For UserWithFilterMessageBodyContainsQuery if message body contains "20" move to folder "20" with fid 9
    # For UserWithFilterMessageBodyNotContainsQuery if message body doesn't contains "тело письма фильтра" move to folder "21" with fid 11
    # For UserWithFilterMessageBodyMatchesQuery if message body matches "тело письма фильтра 18" move to folder "18" with fid 9

    sender, rcpt = get_users("DefaultSender", user)
    msg_id = send_plain_text_message(sender, rcpt.email, text=text)

    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    fid = find_mail_box_row_by_message_id(rcpt.uid, msg_id).fid
    assert fid == expected_fid, "Message for {0} should be delivered to fid {1}".format(rcpt.email, expected_fid)


@pytest.mark.mxfront
@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.parametrize("user, path, eml, expected_fid", [
    ("UserWithFilterContainsAttachNameQuery", "filters", "attach_abook_txt.eml", 11),
    ("UserWithFilterNotContainsAttachNameQuery", "attach_detection_yes", "attach_alternative.eml", 13),
    ("UserWithFilterMatchesAttachNameQuery", "filters", "attach_1_jpg.eml", 13),
    ("UserWithFilterNotMatchesAttachNameQuery", "filters", "attach_abook_txt.eml", 15)])
def test_rules_with_attach_name_queries(user, path, eml, expected_fid):
    # For UserWithFilterContainsAttachNameQuery if attachment name contains "abook.txt" move to folder "24" with fid 11 and ignore other filters
    # For UserWithFilterNotContainsAttachNameQuery if attachment name doesn't contains "Книга1.xls" move to folder "25" with fid 13
    # For UserWithFilterMatchesAttachNameQuery if attachment name matches "1.jpg" move to folder "22" with fid 13
    # For UserWithFilterNotMatchesAttachNameQuery if attachment name doesn't matches "Бесполезно -2003.doc" move to folder "23" with fid 15

    sender, rcpt = get_users("DefaultSender", user)
    msg_id = make_msgid()
    subject = get_random_string(50)
    fd = open(get_letter_path(path, eml))
    msg = build_message_from_file(fd, rcpt.email, {Header.SUBJECT: subject, Header.MESSAGE_ID: msg_id})
    send_letter(sender, rcpt.email, msg)

    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    fid = find_mail_box_row_by_message_id(rcpt.uid, msg_id).fid
    assert fid == expected_fid, "Message for {0} should be delivered to fid {1}".format(rcpt.email, expected_fid)
