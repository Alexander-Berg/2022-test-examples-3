# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date, time

import pytest
from django.conf import settings

from common.tester.testcase import TestCase
from common.tester import transaction_context
from common.tester.factories import create_station, create_thread
from stationschedule import get_schedule_class


@pytest.fixture(scope='class')
def load_suburban_schedule(request):
    old_now = getattr(settings, 'ENVIRONMENT_NOW', None)
    settings.ENVIRONMENT_NOW = datetime(2015, 1, 1)
    atomic = transaction_context.enter_atomic()

    schedule = {}

    schedule['station_1'] = create_station(t_type='suburban')
    schedule['station_2'] = create_station(t_type='suburban')
    schedule['station_3'] = create_station(t_type='suburban')

    schedule['thread'] = create_thread({
        'tz_start_time': time(10),
        't_type': 'suburban',
        'schedule_v1': [
            [None,    0, schedule['station_1']],
            [120,  130, schedule['station_2']],
            [240, None, schedule['station_3']],
        ]
    })

    thread = schedule['thread']

    request.cls.suburban_schedule = schedule

    request.cls.thread = thread

    def fin():
        settings.ENVIRONMENT_NOW = old_now

        transaction_context.rollback_atomic(atomic)

    request.addfinalizer(fin)


@pytest.mark.usefixtures('load_suburban_schedule')
class TestShowInAllDaysPages(TestCase):
    def testNotShowInAllDays(self):
        self.thread.show_in_alldays_pages = False
        self.thread.save()

        station = self.suburban_schedule['station_2']

        schedule_class = get_schedule_class(station, schedule_type='suburban')

        schedule = schedule_class(station, schedule_date=date(2015, 1, 1))
        schedule.build()

        assert len(schedule.schedule_routes) == 1

        schedule = schedule_class(station, schedule_date=None)
        schedule.build()

        assert len(schedule.schedule_routes) == 0

    def testShowInAllDaysPages(self):
        self.thread.show_in_alldays_pages = True
        self.thread.save()

        station = self.suburban_schedule['station_2']

        schedule_class = get_schedule_class(station, schedule_type='suburban')

        schedule = schedule_class(station, schedule_date=date(2015, 1, 1))
        schedule.build()

        assert len(schedule.schedule_routes) == 1

        schedule = schedule_class(station, schedule_date=None)
        schedule.build()

        assert len(schedule.schedule_routes) == 1
