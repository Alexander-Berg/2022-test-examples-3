# -*- coding: utf-8 -*-
import base64
import email
import os
from collections import namedtuple

import pytest
from hamcrest import assert_that, has_item, is_not

from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_subject
from lib.psql import find_mail_message_row_by_subject, find_mail_box_row_by_mid
from lib.random_generator import get_random_string
from lib.smtp_send_tools import send_letter
from lib.users import get_user
from lib.utils import BASE_DIR
CaseInfo = namedtuple("CaseInfo", ["hint_value", "expected_fid", "expected_lids_matcher",
                                   "expected_seen_flag", "expected_recent_flag",
                                   "expected_deleted_flag", "case_comment"])


@pytest.mark.mxback
@pytest.mark.yaback
@pytest.mark.parametrize("case_info", [
    CaseInfo("lid=7", 1, has_item(7), False, True, False, "Проверка работы lid-параметра X-Yandex-Hint"),
    CaseInfo("lid=2490000000042707409\nfid=4", 4, is_not(has_item(2490000000042707409)), False, True, False,
             "Проверка реакции на некорректный lid"),
    CaseInfo("fid=4", 4, is_not(None), False, True, False, "Проверка работы fid-параметра X-Yandex-Hint"),
    CaseInfo("fid=400\nlid=7", 1, has_item(7), False, True, False, "Проверка реакции на некорректный fid")
])
def test_mail_box_table_fields(case_info):
    input_file = open(os.path.join(BASE_DIR, "fixtures/emls/pq/1.eml"))
    msg = email.message_from_file(input_file)
    rcpt = get_user("TestMailBoxTable")
    subject = get_random_string(50)
    del msg[Header.SUBJECT]
    msg[Header.SUBJECT] = subject
    del msg["To"]
    del msg["CC"]
    del msg["BCC"]
    msg["To"] = rcpt.email
    msg[Header.X_YANDEX_HINT] = base64.encodestring(case_info.hint_value)
    msg[Header.X_YANDEX_SPAM] = "1"

    sender = get_user("DefaultSender")
    send_letter(sender, rcpt.email, msg)

    assert_that(rcpt, has_delivered_message_with_subject(subject))
    mail_message_row = find_mail_message_row_by_subject(rcpt.uid, subject)
    mid = mail_message_row.mid
    mail_box_row = find_mail_box_row_by_mid(rcpt.uid, mid)

    assert mail_box_row.fid == case_info.expected_fid
    assert mail_box_row.tid == mid
    assert_that(mail_box_row.lids, case_info.expected_lids_matcher)
    assert mail_box_row.seen == case_info.expected_seen_flag
