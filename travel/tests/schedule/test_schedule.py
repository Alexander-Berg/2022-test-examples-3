# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import datetime

import mock
from hamcrest import assert_that, has_entries

from travel.rasp.library.python.common23.models.core.schedule.train_schedule_plan import TrainSchedulePlan
from travel.rasp.library.python.common23.models.core.schedule.base_rthread import BaseRThread
from travel.rasp.library.python.common23.tester.factories import create_thread, create_train_schedule_plan
from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment


@mock.patch.object(BaseRThread, 'plain_days_text', return_value='every day')
class TestScheduleText(TestCase):
    def setUp(self):
        super(TestScheduleText, self).setUp()
        self.next_plan = 'next plan'

    @replace_now('2016-10-26 00:00:00')
    def test_L_days_text_dict_schedule_switch(self, m_plain_days_text):
        schedule_plan1 = create_train_schedule_plan(appendix_type=TrainSchedulePlan.APPENDIX_TO,
                                                    start_date=environment.today() - datetime.timedelta(days=10),
                                                    end_date=environment.today())
        schedule_plan2 = create_train_schedule_plan(appendix_type=TrainSchedulePlan.APPENDIX_FROM,
                                                    start_date=environment.today() + datetime.timedelta(days=1),
                                                    end_date=environment.today() + datetime.timedelta(days=10))
        thread1 = create_thread(schedule_plan=schedule_plan1)
        thread2 = create_thread(schedule_plan=schedule_plan2)
        thread3 = create_thread(schedule_plan=schedule_plan2,
                                template_start=environment.today() + datetime.timedelta(days=3))

        days_data = thread1.L_days_text_dict(
            shift=1,
            thread_start_date=environment.today(),
            next_plan=self.next_plan,
            show_days=True)
        assert_that(days_data,
                    has_entries({'days_text': u'every day по 26 октября',
                                 'days_text_short': 'every day',
                                 'schedule_plan_appendix': u'по 26 октября'}))

        days_data = thread2.L_days_text_dict(
            shift=1,
            thread_start_date=environment.today() + datetime.timedelta(days=3),
            next_plan=self.next_plan,
            show_days=True)
        assert_that(days_data,
                    has_entries({'days_text': u'every day с 27 октября',
                                 'days_text_short': 'every day',
                                 'schedule_plan_appendix': u'с 27 октября'}))

        days_data = thread3.L_days_text_dict(
            shift=1,
            thread_start_date=environment.today() + datetime.timedelta(days=4),
            next_plan=self.next_plan,
            show_days=True)
        assert_that(days_data,
                    has_entries({'days_text': 'every day'}))
