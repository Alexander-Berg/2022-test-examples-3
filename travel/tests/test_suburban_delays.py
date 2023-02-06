# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytz
from datetime import datetime, date, timedelta, time

import pytest
from django.conf import settings

from common.tester.testcase import TestCase
from common.tester import transaction_context
from common.tester.factories import create_station, create_thread
from stationschedule import get_schedule_class
from stationschedule.models import ZTablo2


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

    rts = thread.rtstation_set.all()[1]

    z_tablo = ZTablo2()
    z_tablo.thread = thread
    z_tablo.route = thread.route
    z_tablo.station = rts.station
    z_tablo.platform = rts.platform
    z_tablo.company_id = thread.company_id

    z_tablo.route_id = thread.route.id
    z_tablo.route_uid = thread.route.route_uid
    z_tablo.number = thread.number

    z_tablo.t_type_id = thread.t_type_id
    z_tablo.title = thread.title

    z_tablo.original_arrival = datetime(2015, 1, 1, 12, 0)
    z_tablo.original_departure = datetime(2015, 1, 1, 12, 10)
    z_tablo.arrival = z_tablo.original_arrival
    z_tablo.departure = z_tablo.original_departure

    z_tablo.arrival_cancelled = False
    z_tablo.departure_cancelled = False

    z_tablo.start_datetime = datetime(2015, 1, 1, 10, 0)
    z_tablo.utc_start_datetime = thread.pytz.localize(z_tablo.start_datetime).\
        astimezone(pytz.UTC).replace(tzinfo=None)

    z_tablo.is_fuzzy = rts.is_fuzzy

    z_tablo.real_arrival = z_tablo.original_arrival + timedelta(minutes=10)
    z_tablo.real_departure = z_tablo.original_departure + timedelta(minutes=20)

    z_tablo.comment_ru = u'Test comment 2'

    z_tablo.save()

    def fin():
        settings.ENVIRONMENT_NOW = old_now

        transaction_context.rollback_atomic(atomic)

    request.addfinalizer(fin)


@pytest.mark.usefixtures('load_suburban_schedule')
class TestSuburbanDelay(TestCase):
    def testHasDelay(self):
        station = self.suburban_schedule['station_2']

        schedule_class = get_schedule_class(station, schedule_type='suburban')

        schedule = schedule_class(station, schedule_date=date(2015, 1, 1), add_z_tablo=True)
        schedule.build()

        schedule_route = schedule.schedule_routes[0]

        assert schedule_route.z_tablo.real_departure == datetime(2015, 1, 1, 12, 30)
        assert schedule_route.z_tablo.real_arrival == datetime(2015, 1, 1, 12, 10)
        assert schedule_route.z_tablo.L_comment(lang='ru') == u'Test comment 2'

    def testHasNoDelay(self):
        station = self.suburban_schedule['station_2']

        schedule_class = get_schedule_class(station, schedule_type='suburban')

        schedule = schedule_class(station, schedule_date=date(2015, 1, 2), add_z_tablo=True)
        schedule.build()

        schedule_route = schedule.schedule_routes[0]

        assert schedule_route.z_tablo is None
