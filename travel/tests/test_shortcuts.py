# -*- coding: utf-8 -*-

from datetime import datetime, time, timedelta, date
from copy import copy

from django.db.models import Q
from mock import patch
from pytz import timezone

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.route_search.base import PlainSegmentSearch, IntervalSegmentSearch
from travel.avia.library.python.route_search.models import AllDaysRThreadSegment, RThreadSegment, IntervalRThreadSegment
from travel.avia.library.python.common.utils.date import MSK_TZ

from travel.avia.library.python.route_search.shortcuts import find, search_routes, get_loc_search_range_aware, get_nears, find_next

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_station, create_transport_type
from travel.avia.library.python.tester.utils.datetime import replace_now


class TestShortcuts(TestCase):
    def setUp(self):
        self.transport_types = [TransportType.objects.get(pk=i).code
                                for i in [TransportType.TRAIN_ID, TransportType.PLANE_ID]]

        self.interval_transport_types = [TransportType.objects.get(pk=TransportType.BUS_ID).code]

    @staticmethod
    def plain_segm_search(from_dt_aware, to_date_aware, z_tablo, max_count):
        segments = []
        for i in range(2):
            segment = RThreadSegment()
            segment.thread = i
            segments.append(segment)
        return segments

    @staticmethod
    def segm_all_days_search():
        segments = []
        for i in range(2):
            segment = AllDaysRThreadSegment()
            segment.thread = i
            segments.append(segment)
        return segments

    @staticmethod
    def interval_search_by_day(from_dt_aware):
        segments = []
        for i in range(2, 4):
            segment = IntervalRThreadSegment()
            segment.thread = i
            segments.append(segment)
        return segments

    def test_find(self):
        """Проверяем, что gen_from вызывается с нужными аргументами.
        Проверяем корректность полученных из find сегментов."""
        station_from = create_station()
        station_to = create_station()
        MSK_TZ = timezone('Europe/Moscow')
        t_type = create_transport_type(TransportType.TRAIN_ID)
        date_from = datetime(2000, 12, 31).date()
        threads_filter = ~Q(supplier_id__in=[2, 3])

        def gen_from(plain_segment_search, from_dt_aware):
            assert from_dt_aware == MSK_TZ.localize(datetime.combine(date_from, time(0, 0, 0)))
            assert plain_segment_search.point_from == station_from
            assert plain_segment_search.point_to == station_to
            assert plain_segment_search.t_types == [t_type]
            assert plain_segment_search.threads_filter == threads_filter

            for i in range(2):
                segment = RThreadSegment()
                segment.thread = i
                segment.station_from = station_from
                segment.start_dt = date_from
                yield segment

        with patch.object(PlainSegmentSearch, 'gen_from', side_effect=gen_from, autospec=True) as mock:
            segments = list(find(station_from, station_to, date_from, t_type, threads_filter))

            assert len(segments) == 2
            for i, segment in enumerate(segments):
                assert segment.thread == i
                assert segment.station_from == station_from
                assert segment.start_dt == date_from
            assert mock.call_count == 1

    @replace_now('2000-01-10 00:00:00')
    def test_search_routes(self):
        """Проверяем, что методы классов PlainSegmentSearch вызываются с нужными аргументами.
        Проверяем корректность полученных сегментов."""
        station_from = create_station()
        station_to = create_station()
        MSK_TZ = timezone('Europe/Moscow')
        departure_date = MSK_TZ.localize(datetime(2000, 1, 10)).date()
        add_z_tablos = True
        max_count = 5

        # сегменты на конкретную дату
        with patch.object(PlainSegmentSearch, 'search', side_effect=self.plain_segm_search) as mock_search, \
                patch.object(PlainSegmentSearch, 'get_service_types', return_value=copy(self.transport_types)), \
                patch.object(PlainSegmentSearch, 'count', side_effect=[1, 2]):

            segments, nears, services = search_routes(station_from, station_to, include_interval=False,
                                                      departure_date=departure_date, add_z_tablos=add_z_tablos,
                                                      max_count=max_count)
            from_dt = MSK_TZ.localize(datetime.combine(departure_date, time()))
            to_dt = from_dt + timedelta(days=1, hours=4)

            mock_search.assert_called_once_with(from_dt, to_dt, add_z_tablos, max_count=max_count)
            assert services == self.transport_types
            assert nears == {'earlier': {'count': 1, 'date': date(2000, 1, 9)},
                             'later': {'count': 2, 'date': date(2000, 1, 11)}}

            assert len(segments) == 2
            for i, segment in enumerate(segments):
                assert segment.thread == i

        # сегменты на все дни
        with patch.object(PlainSegmentSearch, 'all_days_search', side_effect=self.segm_all_days_search) as mock_search, \
                patch.object(PlainSegmentSearch, 'get_service_types', return_value=copy(self.transport_types)), \
                patch.object(PlainSegmentSearch, 'count', side_effect=[1, 2]):

            segments, nears, services = search_routes(station_from, station_to, include_interval=False, max_count=max_count)
            mock_search.assert_called_once_with()
            assert services == self.transport_types
            assert nears == {'today': {'count': 1, 'date': date(2000, 1, 10)},
                             'tomorrow': {'count': 2, 'date': date(2000, 1, 11)}}

    @replace_now('2000-01-10 00:00:00')
    def test_search_routes_with_interval_segments(self):
        """Проверяем, что методы классов IntervalSegmentSearch вызываются с нужными аргументами.
        Проверяем корректность полученных сегментов."""
        station_from = create_station()
        station_to = create_station()
        MSK_TZ = timezone('Europe/Moscow')
        departure_date = MSK_TZ.localize(datetime(2000, 1, 10)).date()

        # сегменты на конкретную дату
        with patch.object(PlainSegmentSearch, 'search', side_effect=self.plain_segm_search), \
             patch.object(PlainSegmentSearch, 'get_service_types', return_value=copy(self.transport_types)), \
             patch.object(IntervalSegmentSearch, 'get_service_types', return_value=copy(self.interval_transport_types)), \
             patch.object(IntervalSegmentSearch, 'search_by_day', side_effect=self.interval_search_by_day) as mock_search, \
             patch.object(IntervalSegmentSearch, 'count', side_effect=[3, 4]), \
             patch.object(PlainSegmentSearch, 'count', side_effect=[1, 2]):

            segments, nears, services = search_routes(station_from, station_to, departure_date=departure_date,
                                                      include_interval=True)
            mock_search.assert_called_once_with(departure_date)
            assert set(services) == set(self.transport_types + self.interval_transport_types)
            assert nears == {'earlier': {'count': 4, 'date': date(2000, 1, 9)},
                             'later': {'count': 6, 'date': date(2000, 1, 11)}}
            assert len(segments) == 4
            for i, segment in enumerate(segments):
                assert segment.thread == i

        # сегменты на все дни
        with patch.object(PlainSegmentSearch, 'all_days_search', side_effect=self.segm_all_days_search), \
             patch.object(PlainSegmentSearch, 'get_service_types', return_value=copy(self.transport_types)), \
             patch.object(IntervalSegmentSearch, 'get_service_types', return_value=copy(self.interval_transport_types)), \
             patch.object(IntervalSegmentSearch, 'all_days_search', side_effect=self.segm_all_days_search) as mock_search:

            segments, nears, services = search_routes(station_from, station_to, include_interval=True, exact_date=True)
            mock_search.assert_called_once_with()
            assert set(services) == set(self.transport_types + self.interval_transport_types)
            assert len(segments) == 4
            assert nears == {'today': {'count': 0, 'date': date(2000, 1, 10)},
                             'tomorrow': {'count': 0, 'date': date(2000, 1, 11)}}

    def test_get_loc_search_range_aware(self):
        station_from = create_station()
        dt = MSK_TZ.localize(datetime(2000, 1, 1, 1, 1))
        current_start_date = dt.replace(hour=0, minute=0)

        start_date, end_date = get_loc_search_range_aware(station_from, dt, expanded_day=False)
        assert (start_date, end_date) == (current_start_date, current_start_date + timedelta(days=1))

        start_date, end_date = get_loc_search_range_aware(station_from, dt.date(), expanded_day=True)
        assert (start_date, end_date) == (current_start_date, current_start_date + timedelta(days=1, hours=4))

    def test_get_nears(self):
        """Проверяем получение количества сегментов на соседние дни."""
        station_from = create_station()
        station_to = create_station()
        MSK_TZ = timezone('Europe/Moscow')
        departure_date = MSK_TZ.localize(datetime(2000, 1, 10)).date()

        with patch.object(IntervalSegmentSearch, 'count', side_effect=[5, 6]), \
             patch.object(PlainSegmentSearch, 'count', side_effect=[1, 2, 3, 4]):

            plain_search = PlainSegmentSearch(station_from, station_to, t_type=None)
            interval_search = IntervalSegmentSearch(station_from, station_to, t_type=None)

            nears = get_nears(station_from, plain_search, departure_date, interval_search=None)
            assert nears == {'earlier': {'count': 1, 'date': date(2000, 1, 9)},
                             'later': {'count': 2, 'date': date(2000, 1, 11)}}

            nears = get_nears(station_from, plain_search, departure_date, interval_search=interval_search)
            assert nears == {'earlier': {'count': 8, 'date': date(2000, 1, 9)},
                             'later': {'count': 10, 'date': date(2000, 1, 11)}}

    def test_find_next(self):
        """Проверяем, что find вызывается с нужными аргументами.
        Проверяем корректность полученных из find_next сегментов."""
        station_from = create_station()
        station_to = create_station()
        MSK_TZ = timezone('Europe/Moscow')
        t_type = create_transport_type(TransportType.TRAIN_ID)
        dt = MSK_TZ.localize(datetime(2000, 1, 1))

        def f(days):
            def _find(point_from, point_to, date_from, t_type):
                for i, d in enumerate(days):
                    segment = RThreadSegment()
                    segment.thread = i
                    segment.station_from = station_from
                    segment.departure = dt + timedelta(days=d)
                    yield segment
            return _find

        with patch('travel.avia.library.python.route_search.shortcuts.find', autospec=True) as mock:
            mock.side_effect = f([1, 1, 2, 2])
            segments = list(find_next(station_from, station_to, dt, t_type, 1))
            mock.assert_called_once_with(station_from, station_to, dt, t_type)

            assert len(segments) == 2
            for i, segment in enumerate(segments):
                assert segment.thread == i
                assert segment.station_from == station_from
                assert segment.departure == dt + timedelta(days=1)

            mock.side_effect = f([10]*10)
            segments = list(find_next(station_from, station_to, dt, t_type, 10))
            assert len(segments) == 1

            mock.side_effect = f([-1]*8 + [1, 1])
            segments = list(find_next(station_from, station_to, dt, t_type, 10))
            assert len(segments) == 10
