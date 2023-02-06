# -*- coding: utf-8 -*-

from datetime import datetime, time
from itertools import chain

import pytest
from django.db.models import Q
from pytz import timezone

from common.models.schedule import RThreadType, RTStation
from common.models.transport import TransportType
from common.tester.testcase import TestCase
from common.tester.factories import create_thread, create_station
from common.tester.utils.datetime import replace_now
from common.utils.date import RunMask, MSK_TIMEZONE

from route_search.base import PlainSegmentSearch
from route_search.tests.utils import has_stationschedule


class TestPlainSegmentSearch(TestCase):
    create_thread = create_thread.mutate(__={'calculate_noderoute': True})

    def test_threads_filter(self):
        station_from = create_station()
        station_to = create_station()

        threads = [  # noqa
            self.create_thread(
                uid=i, supplier={'id': i},
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            )
            for i in range(1, 5)
        ]

        exclude = ~Q(supplier_id__in=[2, 3])
        search = PlainSegmentSearch(station_from, station_to, t_type=None, threads_filter=exclude)
        result = search._get_threads()
        result_threads = chain.from_iterable(result.values())

        assert {'1', '4'} == {t.uid for t in result_threads}

    def test_get_threads_type(self):
        """Проверяем, что нитки с типом CANCEL_ID и INTERVAL_ID не находятся."""
        station_from = create_station()
        station_to = create_station()
        threads = []

        for uid, thread_type in [('basic_uid_1',  RThreadType.BASIC_ID),
                                 ('basic_uid_2', RThreadType.BASIC_ID),
                                 ('cancel_uid', RThreadType.CANCEL_ID),
                                 ('interval_uid', RThreadType.INTERVAL_ID)]:

            threads.append(self.create_thread(
                uid=uid,
                type=thread_type,
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            ))

        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        result = search._get_threads()
        result_threads = chain.from_iterable(result.values())

        assert {'basic_uid_1', 'basic_uid_2'} == {t.uid for t in result_threads}

    def test_get_threads_timezones(self):
        """Проверяем, что ответ группируется по временным зонам отправления ниток."""
        station_from = create_station()
        station_to = create_station()
        threads = []

        threads.append(self.create_thread(
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        ))
        threads.append(self.create_thread(
            schedule_v1=[
                [None, 0, station_from, {'time_zone': 'Asia/Yekaterinburg'}],
                [10, None, station_to],
            ],
        ))
        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        result = search._get_threads()
        assert set(result.keys()) == {'Europe/Moscow', 'Asia/Yekaterinburg'}

    def test_single_zone_presegments(self):
        """Проверяем, что пресегменты отсортированы по времени отправления нитки."""
        station_from = create_station()
        station_to = create_station()
        threads = []
        for start_time, zone in [(time(15, 0), {}),
                                  (time(0, 0), {}),
                                  (time(15, 0), {'time_zone': 'Asia/Yekaterinburg'}),
                                  (time(0, 0), {'time_zone': 'Asia/Yekaterinburg'})]:

            threads.append(self.create_thread(
                tz_start_time=start_time,
                schedule_v1=[
                    [None, 0, station_from, zone],
                    [10, None, station_to],
                ],
            ))

        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        for zone in ['Asia/Yekaterinburg', 'Europe/Moscow']:
            presegms = search.get_single_zone_presegments(zone)
            assert presegms[0] < presegms[1] and presegms[0].rts_from_departure_time < presegms[1].rts_from_departure_time

    @replace_now('2000-01-01 00:00:00')
    def test_single_zone_iter(self):
        """Проверяем, что маска и время отправления учитываются при формировании дней хождения никти."""
        station_from = create_station()
        station_to = create_station()
        threads = []
        dt_from = datetime(2000, 1, 1, 15, tzinfo=timezone('Asia/Yekaterinburg'))
        threads.append(self.create_thread(
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 6)),
            tz_start_time=time(0),
            schedule_v1=[
                [None, 0, station_from, {'time_zone': 'Asia/Yekaterinburg'}],
                [10, None, station_to],
            ],
        ))

        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        result = search._get_threads()
        for zone in result.keys():
            presegms = list(search.get_single_zone_iter(zone, dt_from))
            assert len(presegms) == 4
            assert presegms[0].thread_start_date == datetime(2000, 1, 2).date()
            assert datetime(2000, 1, 6).date() not in [presegm.thread_start_date for presegm in presegms]

    @replace_now('2000-01-01 00:00:00')
    def test_gen_from(self):
        """Проверяем, что сегменты содержат все необходимые атрибуты."""
        station_from = create_station()
        station_to = create_station()
        threads = []

        threads.append(self.create_thread(
            uid='uid_1',
            title='title_1',
            number='number_1',
            supplier={'code': 'code_1'},
            t_type=TransportType.PLANE_ID,
            tz_start_time=time(15),
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 3)),
            schedule_v1=[
                [None, 0, station_from, {'time_zone': 'Asia/Yekaterinburg'}],
                [10, None, station_to],
            ],
        ))
        threads.append(self.create_thread(
            uid='uid_2',
            year_days=RunMask.range(datetime(2000, 2, 1), datetime(2000, 2, 3)),
            schedule_v1=[
                [None, 0, station_from, {'time_zone': 'Asia/Yekaterinburg'}],
                [10, None, station_to],
            ],
        ))

        dt_from = datetime(2000, 1, 1, 15, tzinfo=timezone('Asia/Yekaterinburg'))
        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        segments = list(search.gen_from(dt_from))

        # проверяем даты отправления ниток
        assert segments[0].start_date == datetime(2000, 1, 1).date() and segments[0].thread.uid == 'uid_1'
        assert segments[1].start_date == datetime(2000, 1, 2).date() and segments[1].thread.uid == 'uid_1'
        assert segments[2].start_date == datetime(2000, 2, 1).date() and segments[2].thread.uid == 'uid_2'
        assert segments[3].start_date == datetime(2000, 2, 2).date() and segments[3].thread.uid == 'uid_2'

        threads[0].station_from = station_from
        threads[0].station_to = station_to
        threads[0].rtstation_from = RTStation.objects.get(thread=threads[0], station=station_from)
        threads[0].rtstation_to = RTStation.objects.get(thread=threads[0], station=station_to)

        # проверяем наличие атрибутов у сегмента
        self.check_segment_attributes(segments[0], threads[0])

    @replace_now('2000-02-04 00:00:00')
    def test_get_all_day_presegments_by_zone(self):
        """Проверяем, что маска учитывается при формировании дней хождения никти. Проверяем сортировку."""
        station_from = create_station()
        station_to = create_station()

        create_thread_args = [(time(10), RunMask.range(datetime(2000, 3, 1), datetime(2000, 3, 2)), 0),
                              (time(10), RunMask.range(datetime(2000, 2, 1), datetime(2000, 2, 6)), 5),
                              (time(11), RunMask.range(datetime(2000, 1, 5), datetime(2000, 1, 10)), 5),
                              (time(11), RunMask.range(datetime(2000, 1, 10), datetime(2000, 1, 15)), 5)]

        for start_time, days_mask, departure_time in create_thread_args:
            self.create_thread(
                year_days=days_mask,
                tz_start_time=start_time,
                schedule_v1=[
                    [None, departure_time, station_from],
                    [10, None, station_to],
                ],
            )

        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        threads = search._get_threads()

        expected_dates = [datetime(2000, 3, 1).date(),
                          datetime(2000, 2, 4).date(),
                          datetime(2000, 1, 9).date(),
                          datetime(2000, 1, 14).date()]

        for zone in threads.keys():
            presegms = search.get_all_day_presegments_by_zone(zone)

            assert len(presegms) == 4
            for i, presegm in enumerate(presegms):
                assert presegm.thread_start_date == expected_dates[i]

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
            supplier={'code': 'code_1'},
            t_type=TransportType.PLANE_ID,
            tz_start_time=time(15),
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 12)),
            schedule_v1=[
                [None, 0, station_from, {'time_zone': 'Asia/Yekaterinburg'}],
                [10, None, station_to],
            ],
        ))
        threads.append(self.create_thread(
            uid='uid_2',
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 11)),
            schedule_v1=[
                [None, 0, station_from, {'time_zone': 'Asia/Yekaterinburg'}],
                [10, None, station_to],
            ],
        ))

        search = PlainSegmentSearch(station_from, station_to, t_type=None)
        segments = search.all_days_search()

        assert len(segments) == 2

        # проверяем даты отправления ниток
        assert segments[0].start_date == datetime(2000, 1, 11).date() and segments[0].thread.uid == 'uid_1'
        assert segments[1].start_date == datetime(2000, 1, 10).date() and segments[1].thread.uid == 'uid_2'

        threads[0].station_from = station_from
        threads[0].station_to = station_to
        threads[0].rtstation_from = RTStation.objects.get(thread=threads[0], station=station_from)
        threads[0].rtstation_to = RTStation.objects.get(thread=threads[0], station=station_to)

        # проверяем наличие атрибутов у сегмента
        self.check_segment_attributes(segments[0], threads[0])

    def search_setup(self):
        station_from = create_station()
        station_to = create_station()
        threads = []

        threads.append(self.create_thread(
            uid='uid_1',
            title='title_1',
            number='number_1',
            supplier={'code': 'code_1'},
            t_type=TransportType.PLANE_ID,
            tz_start_time=time(15),
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 7)),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        ))
        threads.append(self.create_thread(
            uid='uid_2',
            t_type=TransportType.PLANE_ID,
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 7)),
            tz_start_time=time(0),
            schedule_v1=[
                [None, 0, station_from],
                [70, None, station_to],
            ],
        ))
        return station_from, station_to, threads

    @replace_now('2000-01-01 00:00:00')
    def test_search(self):
        """Проверяем количество сегментов и добавление к ним информации из ztablo."""
        station_from, station_to, threads = self.search_setup()

        dt_from_aware = datetime(2000, 1, 5, 1, tzinfo=timezone(MSK_TIMEZONE))
        dt_to_aware = datetime(2000, 2, 1, 5, tzinfo=timezone(MSK_TIMEZONE))
        search = PlainSegmentSearch(station_from, station_to, t_type=None)

        # проверяем ограничение на количество возвращаемых сегментов
        max_count = 2
        segments = search.search(dt_from_aware, dt_to_aware, max_count=max_count)

        assert len(segments) == max_count
        assert segments[0].thread.uid == threads[0].uid
        assert segments[1].thread.uid == threads[1].uid

    @has_stationschedule
    @replace_now('2000-01-01 00:00:00')
    def test_search_with_tablo(self):
        station_from, station_to, threads = self.search_setup()

        from stationschedule.models import ZTablo2
        # проверяем добавление полей из ztablo
        ZTablo2.objects.create(
            thread=threads[0],
            station=station_from,
            departure=datetime(2000, 5, 5, 5),
            original_departure=datetime(2000, 1, 5, 15),
        )
        ZTablo2.objects.create(
            thread=threads[1],
            station=station_to,
            arrival=datetime(2000, 5, 5, 5),
            original_arrival=datetime(2000, 1, 6, 1, 10),
        )

        max_count = 3

        dt_from_aware = datetime(2000, 1, 5, 1, tzinfo=timezone(MSK_TIMEZONE))
        dt_to_aware = datetime(2000, 2, 1, 5, tzinfo=timezone(MSK_TIMEZONE))
        search = PlainSegmentSearch(station_from, station_to, t_type=None)

        segments = search.search(dt_from_aware, dt_to_aware, add_z_tablos=True, max_count=max_count)

        assert len(segments) == max_count
        assert isinstance(segments[0].departure_z_tablo, ZTablo2)
        assert isinstance(segments[1].arrival_z_tablo, ZTablo2)
        assert getattr(segments[2], 'departure_z_tablo') is None and getattr(segments[2], 'arrival_z_tablo') is None

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
        threads = []
        MSK_TZ = timezone(MSK_TIMEZONE)

        threads.append(self.create_thread(
            tz_start_time=time(15),
            year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 4)),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        ))
        threads.append(self.create_thread(
            tz_start_time=time(1),
            year_days=RunMask.range(datetime(2000, 2, 1), datetime(2000, 2, 3)),
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ],
        ))

        search = PlainSegmentSearch(station_from, station_to, t_type=None)

        dt = datetime(2000, 1, 1, 15)

        with pytest.raises(ValueError):
            search.count(dt, MSK_TZ.localize(dt))

        with pytest.raises(ValueError):
            search.count(MSK_TZ.localize(dt), dt)

        count = 0
        assert count == search.count(MSK_TZ.localize(datetime(2000, 1, 3, 16)),
                                     MSK_TZ.localize(datetime(2000, 2, 1)))

        count = 3
        assert count == search.count(MSK_TZ.localize(datetime(2000, 1, 1, 16)),
                                     MSK_TZ.localize(datetime(2000, 2, 2)))

    def test_get_service_types(self):
        """Проверяем, что все коды типов транспорта будут учтены."""
        station_from = create_station()
        station_to = create_station()

        transport_type_codes = ['bus', 'helicopter', 'water']

        for t_type_code in transport_type_codes:
            self.create_thread(
                year_days=RunMask.range(datetime(2000, 1, 1), datetime(2000, 1, 3)),
                t_type=t_type_code,
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            )

        search = PlainSegmentSearch(station_from, station_to, t_type=None)

        set(search.get_service_types()) == set(transport_type_codes)

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
        assert not segment.is_fuzzy_from
        assert not segment.is_fuzzy_to
