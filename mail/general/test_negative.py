# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from hamcrest import is_in, is_not, assert_that

from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.smtp_send_tools import send_letter
from lib.users import get_user
from lib.utils import allure_attach
from threading_data import create_test_msg, find_tid_for_user
from threading_negative_scenarios import LABELS_NEGATIVE_SCENARIO, REFERENCES_WITH_TRIVIAL_NEGATIVE_SCENARIO, \
    SUBJ_NEGATIVE_SCENARIO, TICKET_MEDIA_NEGATIVE_SCENARIO_1, TICKET_MEDIA_NEGATIVE_SCENARIO_2, \
    LIVEMAIL_NEGATIVE_SCENARIO, AVIATICKETS_NEGATIVE_SCENARIO, HOTEL_NEGATIVE_SCENARIO, \
    HOTEL_CANCEL_NEGATIVE_SCENARIO, BOUNCE_NEGATIVE_SCENARIO, \
    REFERENCES_WITH_TRIVIAL_AND_SYS_LABELS_NEGATIVE_SCENARIO

thread_ids = []


@pytest.mark.mxback
@pytest.mark.yaback
@pytest.mark.parametrize("scenario", [
    LABELS_NEGATIVE_SCENARIO,  # 0
    SUBJ_NEGATIVE_SCENARIO,  # 1
    REFERENCES_WITH_TRIVIAL_NEGATIVE_SCENARIO,  # 2
    TICKET_MEDIA_NEGATIVE_SCENARIO_1,  # 3
    TICKET_MEDIA_NEGATIVE_SCENARIO_2,  # 4
    LIVEMAIL_NEGATIVE_SCENARIO,  # 5
    AVIATICKETS_NEGATIVE_SCENARIO,  # 6
    HOTEL_NEGATIVE_SCENARIO,  # 7
    HOTEL_CANCEL_NEGATIVE_SCENARIO,  # 8
    BOUNCE_NEGATIVE_SCENARIO,  # 9
    REFERENCES_WITH_TRIVIAL_AND_SYS_LABELS_NEGATIVE_SCENARIO,  # 10
])
def test_threading_with_negative_expected_result(scenario):
    allure_attach(scenario.description, name="Scenario Name")
    rcpt = get_user("TestThreadingNegative")

    for message_info in scenario.message_infos:
        msg = create_test_msg(message_info, rcpt.email)

        send_letter(None, rcpt.email, msg)
        assert_that(rcpt, has_delivered_message_with_msgid(msg[Header.MESSAGE_ID]))
        current_thread_id = find_tid_for_user(rcpt, msg[Header.MESSAGE_ID])
        error_msg = "Зафиксирована ошибочная подклейка письма к треду при сценарии " + scenario.description
        assert_that(current_thread_id, is_not(is_in(thread_ids)), error_msg)
        thread_ids.append(current_thread_id)
