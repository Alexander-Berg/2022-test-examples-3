# -*- coding: utf-8 -*-

import pytz
from datetime import datetime, date, timedelta, time

import pytest
from django.conf import settings

from common.models.transport import TransportType
from common.tester import transaction_context
from common.tester.factories import create_station, create_thread
from common.tester.testcase import TestCase
from route_search.base import PlainSegmentSearch
from route_search.tests.utils import has_stationschedule


@pytest.fixture(scope='class')
def load_schedule(request):
    old_now = getattr(settings, 'ENVIRONMENT_NOW', None)
    settings.ENVIRONMENT_NOW = datetime(2015, 1, 1)

    atomic = transaction_context.enter_atomic()

    schedule = {}

    schedule['station_1'] = create_station()
    schedule['station_2'] = create_station()
    schedule['station_3'] = create_station()

    schedule['thread'] = create_thread({
        '__': {'calculate_noderoute': True},
        't_type': TransportType.PLANE_ID,  # Табло есть только у самолетов
        'tz_start_time': time(10),
        'schedule_v1': [
            [None,    0, schedule['station_1']],
            [120,  130, schedule['station_2']],
            [240, None, schedule['station_3']],
        ]
    })

    thread = schedule['thread']
    rts = thread.rtstation_set.all()[1]

    from stationschedule.models import ZTablo2

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
    z_tablo.lmt = datetime.now()

    z_tablo.save()

    request.cls.schedule = schedule

    def fin():
        settings.ENVIRONMENT_NOW = old_now

        transaction_context.rollback_atomic(atomic)

    request.addfinalizer(fin)


@has_stationschedule
@pytest.mark.usefixtures('load_schedule')
class TestFetchZTablo(TestCase):
    def get_2to3_search(self, date):
        point_from = self.schedule['station_2']
        point_to = self.schedule['station_3']
        from_dt_aware = point_from.pytz.localize(datetime.combine(date, time(0)))
        to_dt_aware = from_dt_aware + timedelta(1)

        return PlainSegmentSearch(point_from, point_to).search(from_dt_aware, to_dt_aware, add_z_tablos=True)

    def get_1to2_search(self, date):
        point_from = self.schedule['station_1']
        point_to = self.schedule['station_2']
        from_dt_aware = point_from.pytz.localize(datetime.combine(date, time(0)))
        to_dt_aware = from_dt_aware + timedelta(1)

        return PlainSegmentSearch(point_from, point_to).search(from_dt_aware, to_dt_aware, add_z_tablos=True)

    def testHasDepartureZTablo(self):
        segments = self.get_2to3_search(date(2015, 1, 1))

        assert len(segments) == 1

        segment = segments[0]
        z_tablo = segment.departure_z_tablo
        assert segment.arrival_z_tablo is None

        assert z_tablo.real_departure == datetime(2015, 1, 1, 12, 30)
        assert z_tablo.real_arrival == datetime(2015, 1, 1, 12, 10)
        assert z_tablo.L_comment(lang='ru') == u'Test comment 2'

    def testHasArrivalZTablo(self):
        segments = self.get_1to2_search(date(2015, 1, 1))

        assert len(segments) == 1

        segment = segments[0]
        z_tablo = segment.arrival_z_tablo
        assert segment.departure_z_tablo is None

        assert z_tablo.real_departure == datetime(2015, 1, 1, 12, 30)
        assert z_tablo.real_arrival == datetime(2015, 1, 1, 12, 10)
        assert z_tablo.L_comment(lang='ru') == u'Test comment 2'

    def testHasNoZTablo(self):
        segments = self.get_2to3_search(date(2015, 1, 2))

        assert len(segments) == 1

        assert segments[0].departure_z_tablo is None
        assert segments[0].arrival_z_tablo is None
