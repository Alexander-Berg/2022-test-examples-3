# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
import datetime

import mock

from travel.rasp.library.python.common23.models.core.schedule.train_schedule_plan import TrainSchedulePlan
from travel.rasp.library.python.common23.tester.factories import create_thread, create_train_schedule_plan
from travel.rasp.library.python.common23.tester.testcase import TestCase


@mock.patch.object(TrainSchedulePlan, 'get_L_appendix', return_value='appendix')
class TestScheduleAppendix(TestCase):
    def setUp(self):
        super(TestScheduleAppendix, self).setUp()
        self.thread_start_date = datetime.date(2015, 1, 1)
        self.next_plan = 'next plan'
        self.appendix = 'appendix'
        self.template_start = datetime.date(2015, 6, 1)
        self.template_end = datetime.date(2015, 8, 31)

    def test_empty_template_start_appendix_from(self, m_get_L_appendix):
        schedule_plan = create_train_schedule_plan(appendix_type=TrainSchedulePlan.APPENDIX_FROM)
        thread = create_thread(schedule_plan=schedule_plan)

        assert thread.L_schedule_plan_appendix_text(self.thread_start_date, self.next_plan) == 'appendix'
        m_get_L_appendix.assert_called_once_with(self.thread_start_date, self.next_plan)

    def test_empty_template_end_appendix_to(self, m_get_L_appendix):
        schedule_plan = create_train_schedule_plan(appendix_type=TrainSchedulePlan.APPENDIX_TO)
        thread = create_thread(schedule_plan=schedule_plan)

        assert thread.L_schedule_plan_appendix_text(self.thread_start_date, self.next_plan) == 'appendix'
        m_get_L_appendix.assert_called_once_with(self.thread_start_date, self.next_plan)

    def test_template_start_appendix_from(self, m_get_L_appendix):
        schedule_plan = create_train_schedule_plan(appendix_type=TrainSchedulePlan.APPENDIX_FROM)
        thread = create_thread(schedule_plan=schedule_plan, template_start=self.template_start)

        assert thread.L_schedule_plan_appendix_text(self.thread_start_date, self.next_plan) is None
        m_get_L_appendix.assert_not_called()

    def test_template_end_appendix_to(self, m_get_L_appendix):
        schedule_plan = create_train_schedule_plan(appendix_type=TrainSchedulePlan.APPENDIX_TO)
        thread = create_thread(schedule_plan=schedule_plan, template_end=self.template_end)

        assert thread.L_schedule_plan_appendix_text(self.thread_start_date, self.next_plan) is None
        m_get_L_appendix.assert_not_called()
