# -*- coding: utf-8 -*-

import pytest
from hamcrest import assert_that

from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.matchers.labels import has_no_user_label, has_user_label
from lib.psql import find_mail_box_row_by_message_id
from lib.random_generator import get_random_string
from lib.smtp_send_tools import send_plain_text_message
from lib.users import get_users


@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.yaback
@pytest.mark.mxfront
@pytest.mark.smtp
def test_user_labels_filter():
    sender, rcpt = get_users("DefaultSender", "UserWithLabelFilters")

    msg_id = send_plain_text_message(sender, rcpt.email, subject="label_red" + get_random_string(5))
    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))

    mail_box_row = find_mail_box_row_by_message_id(rcpt.uid, msg_id)
    assert_that(mail_box_row, has_user_label("red"))


@pytest.mark.mxfront
@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.parametrize("subject_prefix, should_have_label", [
    ("*", True),
    ("sub*ject", True),
    ("subject", False)
])
@pytest.mark.skipif(True, reason="MAILDLV-3438")
def test_rule_with_asterisk(subject_prefix, should_have_label):
    """
    Based on failure: MAILDLV-2652

    Проверяем что правило на * применяется верно.
    У некоторых пользователей на * стояло правило модификации (удаления) письма.
    Из-за неверного применения фильтра * письма у пользователей потерялись.
    """
    sender, rcpt = get_users("DefaultSender", "UserWithLabelFilters")

    msg_id = send_plain_text_message(sender, rcpt.email, subject=subject_prefix + get_random_string(5))
    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))

    mail_box_row = find_mail_box_row_by_message_id(rcpt.uid, msg_id)

    if should_have_label:
        assert_that(mail_box_row, has_user_label("asterisk"))
    else:
        assert_that(mail_box_row, has_no_user_label("asterisk"))
