# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, time

import mock
import pytest
from hamcrest import assert_that, contains, has_properties

from common.apps.suburban_events.forecast.match_cppk import CppkEvent, calculate_cancelled_stations, CppkMatcher
from common.apps.suburban_events.models import MovistaCancelRaw, SuburbanKey
from common.apps.suburban_events.factories import MovistaCancelRawFactory
from common.models.schedule import RThreadType
from common.tester.factories import create_rtstation, create_thread, create_station

create_thread = create_thread.mutate(t_type='suburban')
create_station = create_station.mutate(t_type='suburban')


@pytest.mark.dbuser
def test_calculate_cancelled_stations():
    log = mock.Mock()
    thread = create_thread()
    stations = [create_station(title='station_{}'.format(i)) for i in range(10)]
    thread_path = [create_rtstation(thread=thread, station=st) for st in stations]
    event = CppkEvent(None)
    event.from_station = stations[1]
    event.to_station = stations[-2]
    event.thread_path = thread_path

    calculate_cancelled_stations(event, log)
    assert len(event.cancelled_stations) == 8
    assert event.cancelled_stations == thread_path[1:-1]


@pytest.mark.mongouser
@pytest.mark.dbuser
class TestCppkMatcher(object):
    def test_match_threads(self):
        log = mock.Mock()
        start_station_1, finish_station_1 = create_station(express_id='111'), create_station(express_id='222')
        start_station_2, finish_station_2 = create_station(express_id='333'), create_station(express_id='444')
        from_station_2, to_station_2 = create_station(express_id='331'), create_station(express_id='441')

        thread_1 = create_thread(
            number='thread_1',
            tz_start_time=time(10, 0),
            schedule_v1=[
                [None, 0, start_station_1],
                [10, None, finish_station_1]
            ],
        )
        thread_2 = create_thread(
            number='thread_2',
            tz_start_time=time(15, 16),
            schedule_v1=[
                [None, 0, start_station_2],
                [60, 70, from_station_2],
                [120, 130, to_station_2],
                [180, None, finish_station_2]
            ],
        )
        thread_3 = create_thread(
            number='thread_3',
            tz_start_time=time(23, 16),
            schedule_v1=[
                [None, 0, start_station_2],
                [60, 70, from_station_2],
                [120, 130, to_station_2],
                [180, None, finish_station_2]
            ],
        )

        # проверка того, что обрабатываются только basic-треды
        create_thread(
            type=RThreadType.objects.get(id=RThreadType.CANCEL_ID),
            basic_thread=thread_2,
            number='thread_2',
            tz_start_time=time(15, 16),
            schedule_v1=[
                [None, 0, start_station_2],
                [3, 4, from_station_2],
                [6, 7, to_station_2],
                [10, None, finish_station_2]
            ],
        )

        thread_key_1 = 'thread_1_key'
        thread_key_2 = 'thread_2_key'
        thread_key_3 = 'thread_3_key'
        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)
        SuburbanKey.objects.create(thread=thread_2, key=thread_key_2)
        SuburbanKey.objects.create(thread=thread_3, key=thread_key_3)

        # совпадают станции отправления/прибытия и начала/конца отмены
        MovistaCancelRawFactory(
            create_dt=datetime(2021, 4, 2, 7, 56, 37),
            departure_date=date(2021, 4, 2),
            train_number='thread_1',
            start_express_id=111,
            finish_express_id=222,
            from_express_id=111,
            to_express_id=222
        )
        # разные станциии
        MovistaCancelRawFactory(
            create_dt=datetime(2021, 4, 2, 7, 56, 37),
            departure_date=date(2021, 4, 3),
            train_number='thread_2',
            start_express_id=333,
            finish_express_id=444,
            from_express_id=331,
            to_express_id=441
        )
        # начало и конец не на краях нитки
        MovistaCancelRawFactory(
            create_dt=datetime(2021, 4, 2, 7, 56, 37),
            departure_date=date(2021, 4, 3),
            train_number='thread_3',
            start_express_id=331,
            finish_express_id=441,
            from_express_id=331,
            to_express_id=441
        )

        # одна из станций не нашлась - отбрасываем весь ивент
        MovistaCancelRawFactory(
            create_dt=datetime(2021, 4, 2, 7, 56, 37),
            departure_date=date(2021, 4, 4),
            train_number='thread_4',
            start_express_id=111,
            finish_express_id=222,
            from_express_id=999,
            to_express_id=222
        )
        # нет треда с таким номером - отбрасываем весь ивент
        MovistaCancelRawFactory(
            create_dt=datetime(2021, 4, 2, 7, 56, 37),
            departure_date=date(2021, 4, 5),
            train_number='thread_5',
            start_express_id=111,
            finish_express_id=222,
            from_express_id=111,
            to_express_id=222
        )

        events = list(MovistaCancelRaw.objects.all().order_by('_id').aggregate())

        matcher = CppkMatcher(events, log)
        matched_events = matcher.match()
        assert len(matched_events) == 3

        assert matched_events[0].start_station == start_station_1
        assert matched_events[0].finish_station == finish_station_1
        assert matched_events[0].from_station == start_station_1
        assert matched_events[0].to_station == finish_station_1
        assert matched_events[0].thread == thread_1
        assert matched_events[0].thread_key.thread_key == thread_key_1
        assert matched_events[0].thread_key.thread_start_date == datetime(2021, 4, 2, 10, 0)
        assert matched_events[0].thread_key.thread_type is None
        assert matched_events[0].thread_key.clock_direction is None
        assert [rts.station for rts in matched_events[0].cancelled_stations] == [start_station_1, finish_station_1]
        assert [rts.station for rts in matched_events[0].thread_path] == [start_station_1, finish_station_1]

        assert matched_events[1].start_station == start_station_2
        assert matched_events[1].finish_station == finish_station_2
        assert matched_events[1].from_station == from_station_2
        assert matched_events[1].to_station == to_station_2
        assert matched_events[1].thread == thread_2
        assert matched_events[1].thread_key.thread_key == thread_key_2
        assert matched_events[1].thread_key.thread_start_date == datetime(2021, 4, 3, 15, 16)
        assert matched_events[1].thread_key.thread_type is None
        assert matched_events[1].thread_key.clock_direction is None
        assert [rts.station for rts in matched_events[1].cancelled_stations] == [from_station_2, to_station_2]
        assert [rts.station for rts in matched_events[1].thread_path] == [
            start_station_2, from_station_2, to_station_2, finish_station_2
        ]

        assert matched_events[2].start_station == from_station_2
        assert matched_events[2].finish_station == to_station_2
        assert matched_events[2].from_station == from_station_2
        assert matched_events[2].to_station == to_station_2
        assert matched_events[2].thread == thread_3
        assert matched_events[2].thread_key.thread_key == thread_key_3
        assert matched_events[2].thread_key.thread_start_date == datetime(2021, 4, 2, 23, 16)
        assert matched_events[2].thread_key.thread_type is None
        assert matched_events[2].thread_key.clock_direction is None
        assert [rts.station for rts in matched_events[2].cancelled_stations] == [from_station_2, to_station_2]
        assert [rts.station for rts in matched_events[2].thread_path] == [
            start_station_2, from_station_2, to_station_2, finish_station_2
        ]

    def test_annulled_events_matching(self):
        log = mock.Mock()
        start_station = create_station(express_id='1')
        finish_station = create_station(express_id='2')

        thread = create_thread(
            number='thread',
            tz_start_time=time(10, 0),
            schedule_v1=[
                [None, 0, start_station],
                [10, None, finish_station]
            ],
        )

        thread_key = 'thread_1_key'
        SuburbanKey.objects.create(thread=thread, key=thread_key)

        # совпадают станции отправления/прибытия и начала/конца отмены
        MovistaCancelRawFactory(
            create_dt=datetime(2021, 4, 2, 7, 56, 37),
            departure_date=date(2021, 4, 2),
            train_number='thread',
            start_express_id=1,
            finish_express_id=2,
            from_express_id=None,
            to_express_id=None
        )

        events = list(MovistaCancelRaw.objects.all().order_by('_id').aggregate())

        matcher = CppkMatcher(events, log)
        matched_events = matcher.match()
        assert len(matched_events) == 1

        matched_event = matched_events[0]
        assert matched_event.start_station == start_station
        assert matched_event.finish_station == finish_station
        assert matched_event.from_station is None
        assert matched_event.to_station is None
        assert matched_event.thread == thread
        assert matched_event.thread_key.thread_key == thread_key
        assert matched_event.thread_key.thread_start_date == datetime(2021, 4, 2, 10, 0)
        assert matched_event.thread_key.thread_type is None
        assert matched_event.thread_key.clock_direction is None
        assert [rts.station for rts in matched_event.thread_path] == [start_station, finish_station]
        assert len(matched_event.cancelled_stations) == 0

    def test_wrong_data(self):
        log = mock.Mock()
        start_station, finish_station = create_station(express_id='11'), create_station(express_id='21')
        from_station, to_station = create_station(express_id='12'), create_station(express_id='22')
        middle_station = create_station(express_id='33')

        # тред без to_station
        thread = create_thread(
            number='thread',
            tz_start_time=time(15, 16),
            schedule_v1=[
                [None, 0, start_station],
                [3, 4, from_station],
                [6, 7, middle_station],
                [10, None, finish_station]
            ],
        )

        thread_key = 'thread_key'
        SuburbanKey.objects.create(thread=thread, key=thread_key)

        MovistaCancelRawFactory(
            create_dt=datetime(2021, 4, 2, 7, 56, 37),
            departure_date=date(2021, 4, 3),
            train_number='thread',
            start_express_id=11,
            finish_express_id=21,
            from_express_id=12,
            to_express_id=22
        )
        events = list(MovistaCancelRaw.objects.all().order_by('_id').aggregate())

        matcher = CppkMatcher(events, log)
        matched_events = matcher.match()
        assert len(matched_events) == 0
