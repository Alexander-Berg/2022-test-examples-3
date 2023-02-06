# -*- coding: utf-8 -*-
from __future__ import absolute_import

from datetime import datetime, timedelta

from travel.rasp.api_public.tests.old_versions import ApiTestCase
from travel.rasp.api_public.tests.old_versions.helpers import check_response_invalid_date

from common.tester.factories import create_thread, create_train_schedule_plan
from common.tester.utils.datetime import replace_now


class ApiV1TestCase(ApiTestCase):
    def setUp(self):
        super(ApiV1TestCase, self).setUp()
        self.api_version = 'v1'


class TestThreadInfo(ApiV1TestCase):
    def test_trains_schedule_plan(self):
        plan = create_train_schedule_plan(start_date=datetime.now() + timedelta(days=2))
        current_plan, next_plan = plan.get_current_and_next(datetime.now().date())
        thread = create_thread(
            schedule_plan=plan,
            translated_days_texts=u'[{}, {"ru": "ежедневно"}]')

        thread_json = self.api_get_json('thread', {'uid': thread.uid})
        assert thread_json.get('days') == u'ежедневно ' + next_plan.L_appendix()

    def test_no_trains_schedule_plan(self):
        thread = create_thread(translated_days_texts=u'[{}, {"ru": "ежедневно"}]')

        thread_json = self.api_get_json('thread', {'uid': thread.uid})
        assert thread_json.get('days') == u'ежедневно'

    @replace_now('2001-01-01 00:00:00')
    def test_date_range(self):
        thread = create_thread()
        date = '1920-01-01'
        thread_json = self.api_get('thread', {'uid': thread.uid, 'date': date})
        check_response_invalid_date(thread_json, date)

        date = '2010-01-01'
        thread_json = self.api_get('thread', {'uid': thread.uid, 'date': date})
        check_response_invalid_date(thread_json, date)


class TestSearchView(ApiV1TestCase):
    @replace_now('2001-01-01 00:00:00')
    def test_date_range(self):
        date = '1920-01-01'
        search_json = self.api_get('search', {'date': date})
        check_response_invalid_date(search_json, date)

        date = '2010-01-01'
        search_json = self.api_get('search', {'date': date})
        check_response_invalid_date(search_json, date)


class TestScheduleView(ApiV1TestCase):
    @replace_now('2001-01-01 00:00:00')
    def test_date_range(self):
        date = '1920-01-01'
        schedule_json = self.api_get('schedule', {'date': date})
        check_response_invalid_date(schedule_json, date)

        date = '2010-01-01'
        schedule_json = self.api_get('schedule', {'date': date})
        check_response_invalid_date(schedule_json, date)
