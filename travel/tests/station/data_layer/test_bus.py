# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, time

import pytest
import pytz
from hamcrest import assert_that, contains_inanyorder

from common.models.schedule import RThreadType, RunMask
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement, create_thread, create_transport_subtype
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from stationschedule.type.base import BaseIntervalSchedule
from stationschedule.views import get_schedule_class

from travel.rasp.morda_backend.morda_backend.station.data_layer.bus import (BusStationForPage, BusStationThread)
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_context import StationPageContext
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_type import StationPageType


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True}, t_type=TransportType.BUS_ID, generate_title=True)
create_station = create_station.mutate(t_type=TransportType.BUS_ID)
create_settlement = create_settlement.mutate(country_id=225)
ekb_tz = pytz.timezone('Etc/GMT-5')


@replace_now('2020-02-01')
def test_bus_station_thread():
    station1 = create_station(id=701, t_type=TransportType.BUS_ID)
    station2 = create_station(id=702, t_type=TransportType.BUS_ID)

    create_thread(
        canonical_uid='canonicalUid',
        title='Станция1 - Станция2',
        number='333',
        year_days=[date(2020, 1, 29), date(2020, 2, 1), date(2020, 2, 3)],
        schedule_v1=[[None, 0, station1], [60, None, station2]]
    )

    schedule_cls = get_schedule_class(station1, t_type_code='bus')
    schedule = schedule_cls(station1, all_days_next_days=60, limit=100000)
    schedule.build()

    route = list(schedule.schedule_routes)[0]
    thread = BusStationThread(route)

    assert thread.canonical_uid == 'canonicalUid'
    assert thread.departure_from.isoformat() == '2020-02-01T00:00:00+03:00'


@replace_now('2020-02-01')
def test_bus_station_interval_thread():
    station1 = create_station(id=701, t_type=TransportType.BUS_ID)
    station2 = create_station(id=702, t_type=TransportType.BUS_ID)

    create_thread(
        canonical_uid='canonicalUid',
        title='Станция1 - Станция2',
        number='333',
        year_days=[date(2020, 1, 29), date(2020, 2, 1), date(2020, 2, 3)],
        schedule_v1=[[None, 0, station1], [60, None, station2]],
        type_id=RThreadType.INTERVAL_ID,
        density='Автобус раз в 20 мин.',
        period_int=20,
        begin_time='07:00:00',
        end_time='23:00:00'
    )

    schedule = BaseIntervalSchedule(station1, all_days_next_days=60)
    schedule.build()

    route = list(schedule.schedule_routes)[0]
    thread = BusStationThread(route)

    assert thread.canonical_uid == 'canonicalUid'
    assert hasattr(thread, 'interval')
    assert thread.interval['density'] == 'Автобус раз в 20 мин.'
    assert thread.interval['begin_time'] == '07:00'
    assert thread.interval['end_time'] == '23:00'


def _create_station_for_page(station, date):
    page_type = StationPageType(station, 'bus')
    page_context = StationPageContext(station, date, 'departure', environment.now_aware())
    st_for_page = BusStationForPage(page_type, 'ru')
    st_for_page.load_threads(page_context)

    return st_for_page


@replace_now('2020-02-01')
def test_bus_schedule_blocks():
    settlement0 = create_settlement(title='c0')
    settlement1 = create_settlement(title='c1')

    station0 = create_station(id=700, title='s0', time_zone=ekb_tz, settlement=settlement0)
    station1 = create_station(id=701, title='s1', type_choices='bus', time_zone=ekb_tz)
    station2 = create_station(id=702, title='s2', time_zone=ekb_tz, settlement=settlement1)

    bus_subtype = create_transport_subtype(t_type=TransportType.BUS_ID, code='to', title_ru='Маршрутка')

    create_thread(
        canonical_uid='c02-1',
        title='02',
        number='128',
        year_days=[date(2020, 1, 29), date(2020, 2, 1), date(2020, 2, 3)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(1, 0),
        schedule_v1=[
            [None, 0, station0],
            [60, 70, station1, {'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='c02-2',
        title='02',
        number='128',
        year_days=RunMask.ALL_YEAR_DAYS,
        template_text='ежедневно',
        time_zone='Etc/GMT-3',
        tz_start_time=time(2, 0),
        schedule_v1=[
            [None, 0, station0],
            [60, 70, station1, {'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='c02-3',
        title='02',
        number='128',
        year_days=RunMask.ALL_YEAR_DAYS,
        template_text='ежедневно',
        time_zone='Etc/GMT-3',
        tz_start_time=time(4, 0),
        schedule_v1=[
            [None, 0, station0],
            [60, 70, station1, {'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='int-c12-1',
        number='256а',
        year_days=[date(2020, 2, 1), date(2020, 2, 3)],
        time_zone='Etc/GMT-3',
        t_subtype=bus_subtype,
        schedule_v1=[
            [None, 0, station1],
            [120, None, station2]
        ],
        type_id=RThreadType.INTERVAL_ID,
        density='интервал 20 мин.',
        begin_time='06:00:00',
        end_time='21:00:00'
    )

    create_thread(
        canonical_uid='int-c12-2',
        number='256а',
        year_days=[date(2020, 2, 2)],
        time_zone='Etc/GMT-3',
        t_subtype=bus_subtype,
        schedule_v1=[
            [None, 0, station1],
            [120, None, station2]
        ],
        type_id=RThreadType.INTERVAL_ID,
        density='интервал 20 мин.',
        begin_time='06:30:00',
        end_time='21:00:00'
    )

    create_thread(
        canonical_uid='int-c02',
        number='256',
        year_days=[date(2020, 2, 4)],
        time_zone='Etc/GMT-3',
        schedule_v1=[
            [None, 0, station0],
            [60, 70, station1],
            [120, None, station2]
        ],
        type_id=RThreadType.INTERVAL_ID,
        density='интервал 20 мин.',
        begin_time='07:00:00',
        end_time='23:00:00'
    )

    # блоки распианий на все дни

    st_for_page = _create_station_for_page(station1, 'all-days')
    st_for_page.threads_smart_sort()

    schedule_blocks = st_for_page.schedule_blocks
    assert len(schedule_blocks) == 3

    # блок обычных ниток c number='128' title='02'

    block = schedule_blocks[0]
    assert block.number == '128'
    assert block.title == '02'
    # assert block['t_type'] == 'bus'

    schedule = block.schedule
    assert len(schedule) == 2

    assert schedule[0].days_text == 'ежедневно'
    threads = schedule[0].threads
    assert len(threads) == 2
    assert threads[0].canonical_uid == 'c02-2'
    assert threads[1].canonical_uid == 'c02-3'

    assert schedule[1].days_text == 'только 29 января, 1, 3 февраля'
    threads = schedule[1].threads
    assert len(threads) == 1
    assert threads[0].canonical_uid == 'c02-1'

    # блок интервальных ниток с number='256' title='int'

    block = schedule_blocks[1]
    assert block.number == '256'
    assert block.title == 'c0\xa0\u2014 c1'
    assert block.t_type == 'bus'

    schedule = block.schedule
    assert len(schedule) == 1

    assert schedule[0].days_text == 'только 4 февраля'
    threads = schedule[0].threads
    assert len(threads) == 1
    assert threads[0].canonical_uid == 'int-c02'

    # блок интервальных ниток с number='256a' title='int'

    block = schedule_blocks[2]
    assert block.number == '256а'
    assert block.title == 'c1'
    assert block.t_type == 'bus'

    schedule = block.schedule
    assert len(schedule) == 2

    assert schedule[0].days_text == 'только 1, 3 февраля'
    threads = schedule[0].threads
    assert len(threads) == 1
    assert threads[0].canonical_uid == 'int-c12-1'

    assert schedule[1].days_text == 'только 2 февраля'
    threads = schedule[1].threads
    assert len(threads) == 1
    assert threads[0].canonical_uid == 'int-c12-2'

    # блоки расписаний на конкретный день '2020-02-01'

    st_for_page = _create_station_for_page(station1, '2020-02-01')
    st_for_page.threads_smart_sort()

    schedule_blocks = st_for_page.schedule_blocks
    assert len(schedule_blocks) == 2

    # блок обычных ниток c number='128' title='02'

    block = schedule_blocks[0]
    assert block.number == '128'
    assert block.title == '02'
    assert block.t_type == 'bus'

    schedule = block.schedule
    assert len(schedule) == 1

    assert not hasattr(schedule[0], 'days_text')
    threads = schedule[0].threads
    assert len(threads) == 3
    assert threads[0].canonical_uid == 'c02-1'
    assert threads[1].canonical_uid == 'c02-2'
    assert threads[2].canonical_uid == 'c02-3'

    # блок интервальных ниток с number='256a' title='int'

    block = schedule_blocks[1]
    assert block.number == '256а'
    assert block.title == 'c1'
    assert block.t_type == 'bus'

    schedule = block.schedule
    assert len(schedule) == 1

    assert not hasattr(schedule[0], 'days_text')
    threads = schedule[0].threads
    assert len(threads) == 1
    assert threads[0].canonical_uid == 'int-c12-1'


def test_stops():
    settlement0 = create_settlement(title='c0')
    settlement1 = create_settlement(title='c1')

    station0 = create_station(id=700, title='st0', majority=1, settlement=settlement0)
    station1 = create_station(id=701, title='st1', majority=2, settlement=settlement0)
    station2 = create_station(id=702, title='st2', majority=3)
    station3 = create_station(id=703, title='st3', majority=5)
    station4 = create_station(id=704, title='st4', majority=2, settlement=settlement1)

    create_thread(
        canonical_uid='t1',
        title='t1',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [10, 20, station1],
            [30, 40, station2],
            [50, 60, station3],
            [70, None, station4]
        ]
    )

    create_thread(
        canonical_uid='t2',
        title='t2',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [60, 70, station1],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='t3',
        title='t3',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [120, None, station4]
        ]
    )

    create_thread(
        canonical_uid='t4',
        title='t4',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station3],
            [120, None, station0]
        ]
    )

    # station0

    st_for_page = _create_station_for_page(station0, 'all-days')
    st_for_page.make_stops()
    stops = sorted(st_for_page.stops, key=lambda s: s.title)

    assert len(stops) == 3

    stop = stops[0]
    assert stop.id == 701
    assert stop.title == 'st1'
    assert stop.settlement == 'c0'
    assert stop.majority == 2
    assert_that(stop.threads, contains_inanyorder('t1', 't2'))

    stop = stops[1]
    assert stop.id == 702
    assert stop.title == 'st2'
    assert stop.majority == 3
    assert_that(stop.threads, contains_inanyorder('t1', 't2'))

    stop = stops[2]
    assert stop.id == 704
    assert stop.title == 'st4'
    assert stop.majority == 2
    assert stop.settlement == 'c1'
    assert_that(stop.threads, contains_inanyorder('t1', 't3'))

    # station3

    st_for_page = _create_station_for_page(station3, 'all-days')
    st_for_page.make_stops()
    stops = sorted(st_for_page.stops, key=lambda s: s.title)

    assert len(stops) == 2

    stop = stops[0]
    assert stop.id == 700
    assert stop.title == 'st0'
    assert stop.settlement == 'c0'
    assert stop.majority == 1
    assert_that(stop.threads, contains_inanyorder('t4'))

    stop = stops[1]
    assert stop.id == 704
    assert stop.title == 'st4'
    assert stop.settlement == 'c1'
    assert stop.majority == 2
    assert_that(stop.threads, contains_inanyorder('t1'))

    # station4

    st_for_page = _create_station_for_page(station4, 'all-days')
    st_for_page.make_stops()
    stops = sorted(st_for_page.stops, key=lambda s: s.title)

    assert len(stops) == 0


def test_schedule_block_title():
    settlement0 = create_settlement(title='c0')
    settlement1 = create_settlement(title='c1')

    station0 = create_station(id=700, title='st0', settlement=settlement0)
    station5 = create_station(id=704, title='st_in_city', settlement=settlement0)
    station6 = create_station(id=705, title='st_in_another_city', settlement=settlement1)
    station1 = create_station(id=701, title='st1', settlement=settlement1)
    station2 = create_station(id=702, title='st2', settlement=settlement1, not_generalize=True)
    station3 = create_station(id=703, title='st3')

    create_thread(
        canonical_uid='t1',
        number='t1',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [70, None, station1]
        ]
    )

    create_thread(
        canonical_uid='t2',
        number='t2',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [70, None, station2]
        ]
    )

    create_thread(
        canonical_uid='t3',
        number='t3',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [70, None, station3]
        ]
    )

    create_thread(
        canonical_uid='t4',
        number='t4',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station1],
            [10, 20, station0],
            [30, 40, station2],
            [70, None, station3]
        ]
    )

    create_thread(
        canonical_uid='t5',
        number='t5',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [70, None, station5]
        ],
        title_strategy='mta'
    )

    create_thread(
        canonical_uid='t6',
        number='t6',
        year_days=RunMask.ALL_YEAR_DAYS,
        schedule_v1=[
            [None, 0, station0],
            [70, None, station6]
        ],
        title_strategy='mta'
    )

    st_for_page = _create_station_for_page(station0, 'all-days')
    st_for_page.threads_smart_sort()

    schedule_blocks = st_for_page.schedule_blocks

    # обощение до города
    block = schedule_blocks[0]
    assert block.number == 't1'
    assert block.title == 'c1'

    # станция которая не обобщается то города
    block = schedule_blocks[1]
    assert block.number == 't2'
    assert block.title == 'st2'

    # станция без города
    block = schedule_blocks[2]
    assert block.number == 't3'
    assert block.title == 'st3'

    # проходящий рейс
    block = schedule_blocks[3]
    assert block.number == 't4'
    assert block.title == 'c1\xa0\u2014 st3'

    # в том же городе
    block = schedule_blocks[4]
    assert block.number == 't5'
    assert block.title == 'c0 (st_in_city)'

    # в другом городе
    block = schedule_blocks[5]
    assert block.number == 't6'
    assert block.title == 'c1'
