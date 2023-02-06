# -*- coding: utf-8 -*-

from datetime import datetime, time, timedelta

from django.db.models import Q
from pytz import timezone

from common.models.schedule import RThreadType, RTStation
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now
from common.utils.date import RunMask, MSK_TIMEZONE
from route_search.base import IntervalSegmentSearch


class TestIntervalSegmentSearch(TestCase):
    create_thread = create_thread.mutate(__={'calculate_noderoute': True})

    def test_get_threads_type(self):
        """Проверяем, что находятся только интервальные нитки."""
        station_from = create_station()
        station_to = create_station()
        threads = []

        for uid, thread_type in [('interval_uid_1',  RThreadType.INTERVAL_ID),
                                 ('interval_uid_2', RThreadType.INTERVAL_ID),
                                 ('cancel_uid', RThreadType.CANCEL_ID),
                                 ('basic_uid', RThreadType.BASIC_ID)]:
            threads.append(self.create_thread(
                uid=uid,
                type=thread_type,
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            ))

        search = IntervalSegmentSearch(station_from, station_to, t_type=None)
        threads = search._get_threads()

        assert {'interval_uid_1', 'interval_uid_2'} == {t.uid for t in threads}

    def test_get_threads_sort_by_begin_time(self):
        """Проверяем, что ответ отсортирован по begin_time."""
        station_from = create_station()
        station_to = create_station()
        threads = []
        for uid, thread_type, begin_time in [('interval_uid_1', RThreadType.INTERVAL_ID, time(3)),
                                             ('interval_uid_2', RThreadType.INTERVAL_ID, time(1)),
                                             ('interval_uid_3', RThreadType.INTERVAL_ID, time(2))]:
            threads.append(self.create_thread(
                uid=uid,
                type=thread_type,
                begin_time=begin_time,
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            ))

        search = IntervalSegmentSearch(station_from, station_to, t_type=None)
        threads = search._get_threads()
        assert [thread.uid for thread in threads] == ['interval_uid_2', 'interval_uid_3', 'interval_uid_1']

    @replace_now('2000-01-01 00:00:00')
    def test_search_by_day(self):
        """Проверяем, что учитываются дни хождения нитки.
        Проверяем, что сегменты содержат необходимые атрибуты."""
        station_from = create_station()
        station_to = create_station()
        loc_date = datetime(2000, 1, 10).date()
        threads = []

        threads.append(self.create_thread(
            uid='uid_1',
            title='title_1',
            type=RThreadType.INTERVAL_ID,
            begin_time=time(20),
            end_time=time(23),
            number='number_1',
            supplier={'code': 'code_1'},
            t_type=TransportType.PLANE_ID,
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 12)),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        ))
        threads.append(self.create_thread(
            uid='uid_2',
            type=RThreadType.INTERVAL_ID,
            begin_time=time(1),
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 10)),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        ))

        search = IntervalSegmentSearch(station_from, station_to, t_type=None)
        segments = search.search_by_day(loc_date)

        assert len(segments) == 1

        assert segments[0].last_departure == station_from.pytz.localize(datetime.combine(loc_date, threads[0].end_time))
        assert segments[0].duration == timedelta(minutes=10)
        assert segments[0].interval_thread_departure_from_date == loc_date

        threads[0].station_from = station_from
        threads[0].station_to = station_to
        threads[0].rtstation_from = RTStation.objects.get(thread=threads[0], station=station_from)
        threads[0].rtstation_to = RTStation.objects.get(thread=threads[0], station=station_to)

        # проверяем наличие атрибутов у сегмента
        self.check_segment_attributes(segments[0], threads[0])

    @replace_now('2000-01-11 00:00:00')
    def test_all_days_search(self):
        """Проверяем, что сегменты содержат необходимые атрибуты."""
        station_from = create_station()
        station_to = create_station()
        threads = []

        threads.append(self.create_thread(
            uid='uid_1',
            title='title_1',
            number='number_1',
            type=RThreadType.INTERVAL_ID,
            supplier={'code': 'code_1'},
            t_type=TransportType.PLANE_ID,
            begin_time=time(0),
            end_time=time(23),
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 12)),
            schedule_v1=[
                [None, 0, station_from],
                [5, None, station_to],
            ],
        ))
        threads.append(self.create_thread(
            uid='uid_2',
            type=RThreadType.INTERVAL_ID,
            begin_time=time(0),
            end_time=time(20),
            year_days=RunMask.range(datetime(2000, 2, 2), datetime(2000, 2, 11)),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        ))

        search = IntervalSegmentSearch(station_from, station_to, t_type=None)
        segments = search.all_days_search()

        assert len(segments) == 2

        assert segments[0].duration == timedelta(minutes=5)
        assert segments[1].duration == timedelta(minutes=10)

        threads[0].station_from = station_from
        threads[0].station_to = station_to
        threads[0].rtstation_from = RTStation.objects.get(thread=threads[0], station=station_from)
        threads[0].rtstation_to = RTStation.objects.get(thread=threads[0], station=station_to)

        # проверяем наличие атрибутов у сегмента
        self.check_segment_attributes(segments[0], threads[0])

    @replace_now('2000-01-01 00:00:00')
    def test_count(self):
        """Проверяем, что все сегменты, удовлетворяющие времени отправления, будут учтены."""
        station_from = create_station()
        station_to = create_station()
        MSK_TZ = timezone(MSK_TIMEZONE)

        self.create_thread(
            begin_time=time(0),
            end_time=time(1),
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 4)),
            type=RThreadType.INTERVAL_ID,
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        )
        self.create_thread(
            begin_time=time(19),
            end_time=time(20),
            year_days=RunMask.range(datetime(2000, 1, 3), datetime(2000, 2, 3)),
            type=RThreadType.INTERVAL_ID,
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        )

        search = IntervalSegmentSearch(station_from, station_to, t_type=None)

        assert search.count(MSK_TZ.localize(datetime(2000, 1, 3, 16))) == 2

    def test_get_service_types(self):
        """Проверяем, что все коды типов транспорта будут учтены."""
        station_from = create_station()
        station_to = create_station()

        transport_type_codes = ['bus', 'helicopter', 'water']

        for t_type_code in transport_type_codes:
            self.create_thread(
                begin_time=time(0),
                end_time=time(1),
                year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 4)),
                type=RThreadType.INTERVAL_ID,
                t_type=t_type_code,
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            )

        search = IntervalSegmentSearch(station_from, station_to, t_type=None)

        set(search.get_service_types()) == set(transport_type_codes)

    def test_threads_filter(self):
        station_from = create_station()
        station_to = create_station()

        for i in range(1, 5):
            self.create_thread(
                uid=i,
                type=RThreadType.INTERVAL_ID,
                supplier={'id': i},
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            )

        exclude = ~Q(supplier_id__in=[2, 3])
        search = IntervalSegmentSearch(station_from, station_to, t_type=None, threads_filter=exclude)
        result_threads = search._get_threads()
        assert {'1', '4'} == {t.uid for t in result_threads}

    @staticmethod
    def check_segment_attributes(segment, thread):
        assert segment.title == thread.title
        assert segment.number == thread.number
        assert segment.t_type == thread.t_type
        assert segment.supplier_code == thread.supplier.code
        assert segment.station_from == thread.station_from
        assert segment.station_to == thread.station_to
        assert segment.rtstation_from == thread.rtstation_from
        assert segment.rtstation_to == thread.rtstation_to
        assert segment.thread == thread
