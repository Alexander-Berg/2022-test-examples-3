# -*- coding: utf-8 -*-
from datetime import datetime

import mock

from travel.rasp.api_public.api_public.old_versions.core.train_schedule import set_train_schedule_plan
from common.tester.testcase import TestCase
from common.tester.factories import create_rthread_segment, create_train_schedule_plan
from common.tester.utils.datetime import replace_now


TEST_NOW = datetime(2014, 11, 30)


class TestSetTrainSchedulePlan(TestCase):

    @replace_now(TEST_NOW)
    def test_valid(self):
        segments = [create_rthread_segment() for _ in range(3)]
        result_threads = set(s.thread for s in segments)

        class A(object):
            """ Любой объект без атрибута thread. """

        segments.append(A())

        with mock.patch('common.models.schedule.TrainSchedulePlan.add_to_threads') as m_add:
            plans = (create_train_schedule_plan(), create_train_schedule_plan())
            m_add.return_value = plans

            set_train_schedule_plan(segments)

            assert all(s.next_plan == plans[1] for s in segments)

            assert len(m_add.call_args_list) == 1
            threads, dt = m_add.call_args_list[0][0]
            assert set(threads) == result_threads
            assert dt == TEST_NOW.date()
