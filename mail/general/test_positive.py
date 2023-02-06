# -*- coding: utf-8 -*-


from __future__ import unicode_literals

import pytest
from hamcrest import assert_that, equal_to

from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.smtp_send_tools import send_letter
from lib.users import get_user
from lib.utils import allure_attach
from threading_data import create_test_msg, find_tid_for_user
from threading_positive_scenarios import SAME_SUBJECTS_SCENARIO, SAME_SUBJECTS_WITH_REFS_SCENARIO, \
    SAME_SUBJECTS_WITH_REFS_AND_LABELS_SCENARIO, SAME_SUBJECTS_WITH_REFS_AND_LABELS_2_SCENARIO, \
    SAME_SUBJECTS_WITH_SAME_REFS_SCENARIO, SAME_REFERENCES_WITH_LABELS_SCENARIO_1, \
    SAME_REFERENCES_WITH_LABELS_SCENARIO_2, SAME_REFERENCES_WITH_LABELS_SCENARIO_3, \
    SAME_REFERENCES_WITH_LABELS_AND_TRIVIAL_SUBJECTS, SAME_REFERENCES_WITH_TRIVIAL_AND_SYS_LABELS, \
    SAME_REFERENCES_WITH_TRIVIAL, SAME_REFERENCES_FOR_TICKETS_MEDIA_1, SAME_REFERENCES_FOR_TICKETS_MEDIA_2, \
    SAME_REFERENCES_FOR_LIVEMAIL, SAME_REFERENCES_WITH_TRIVIAL_AND_SYS_2_LABELS


class TestThreadingPositive(object):
    @pytest.mark.mxback
    @pytest.mark.yaback
    @pytest.mark.parametrize("scenario", [
        SAME_SUBJECTS_SCENARIO,  # 0
        SAME_SUBJECTS_WITH_REFS_SCENARIO,  # 1
        SAME_SUBJECTS_WITH_REFS_AND_LABELS_SCENARIO,  # 2
        SAME_SUBJECTS_WITH_REFS_AND_LABELS_2_SCENARIO,  # 3
        SAME_SUBJECTS_WITH_SAME_REFS_SCENARIO,  # 4
        SAME_REFERENCES_WITH_LABELS_SCENARIO_1,  # 5
        SAME_REFERENCES_WITH_LABELS_SCENARIO_2,  # 6
        SAME_REFERENCES_WITH_LABELS_SCENARIO_3,  # 7
        SAME_REFERENCES_WITH_LABELS_AND_TRIVIAL_SUBJECTS,  # 8
        SAME_REFERENCES_WITH_TRIVIAL_AND_SYS_LABELS,  # 9
        SAME_REFERENCES_WITH_TRIVIAL_AND_SYS_2_LABELS,  # 10
        SAME_REFERENCES_WITH_TRIVIAL,  # 11
        SAME_REFERENCES_FOR_TICKETS_MEDIA_1,  # 12
        SAME_REFERENCES_FOR_TICKETS_MEDIA_2,  # 13
        SAME_REFERENCES_FOR_LIVEMAIL  # 14
    ])
    def test_threading_with_positive_expected_result(self, scenario):
        allure_attach(scenario.description, name="Scenario Name")
        rcpt = get_user("TestThreadingPositive")

        global_thread_id = ""
        for message_info in scenario.message_infos:
            msg = create_test_msg(message_info, rcpt.email)
            send_letter(None, rcpt.email, msg)
            assert_that(rcpt, has_delivered_message_with_msgid(msg[Header.MESSAGE_ID]))
            current_thread_id = find_tid_for_user(rcpt, msg[Header.MESSAGE_ID])
            if global_thread_id:
                error_msg = "Письма не склеились в тред при сценарии " + scenario.description
                assert_that(current_thread_id, equal_to(global_thread_id), error_msg)
            global_thread_id = current_thread_id
