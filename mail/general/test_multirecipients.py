# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from hamcrest import assert_that, equal_to

from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.smtp_send_tools import send_letter
from lib.users import get_users
from lib.utils import allure_attach
from tests.threading.threading_data import find_tid_for_user, create_test_msg
from threading_positive_scenarios import SAME_SUBJECTS_SCENARIO, SAME_SUBJECTS_WITH_REFS_SCENARIO


@pytest.mark.mxback
@pytest.mark.yaback
@pytest.mark.parametrize("scenario", [
    SAME_SUBJECTS_SCENARIO,
    SAME_SUBJECTS_WITH_REFS_SCENARIO
])
def test_threading_different_rcpts(scenario):
    allure_attach(scenario.description, name="Scenario Name")
    rcpt_1, rcpt_2, rcpt_3 = get_users("TestThreadingMultiRecipients1", "TestThreadingMultiRecipients2",
                                       "TestThreadingMultiRecipients3")
    thread_id_1 = ""
    thread_id_2 = ""
    thread_id_3 = ""

    for message_info in scenario.message_infos:
        msg = create_test_msg(message_info, rcpt_1.email)
        msg["CC"] = rcpt_2.email
        send_letter(None, [rcpt_1.email, rcpt_2.email, rcpt_3.email], msg)
        assert_that(rcpt_1, has_delivered_message_with_msgid(msg[Header.MESSAGE_ID]))
        assert_that(rcpt_2, has_delivered_message_with_msgid(msg[Header.MESSAGE_ID]))
        assert_that(rcpt_3, has_delivered_message_with_msgid(msg[Header.MESSAGE_ID]))

        thread_id = find_tid_for_user(rcpt_1, msg[Header.MESSAGE_ID])
        if thread_id_1:
            error_msg = "Письма не склеились в тред для первого получателя при сценарии " + scenario.description
            assert_that(thread_id, equal_to(thread_id_1), error_msg)
        thread_id_1 = thread_id

        thread_id = find_tid_for_user(rcpt_2, msg[Header.MESSAGE_ID])
        if thread_id_2:
            error_msg = "Письма не склеились в тред для второго (СС) получателя при сценарии " + scenario.description
            assert_that(thread_id, equal_to(thread_id_2), error_msg)
        thread_id_2 = thread_id

        thread_id = find_tid_for_user(rcpt_3, msg[Header.MESSAGE_ID])
        if thread_id_3:
            error_msg = "Письма не склеились в тред для второго (BСС) получателя при сценарии " + scenario.description
            assert_that(thread_id, equal_to(thread_id_3), error_msg)
        thread_id_3 = thread_id
