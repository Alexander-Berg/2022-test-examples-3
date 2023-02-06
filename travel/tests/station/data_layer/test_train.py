# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, time

import mock
import pytest
import pytz
from hamcrest import has_entries, assert_that

from common.models.schedule import DeLuxeTrain
from common.models.transport import TransportType
from common.tester.factories import create_station, create_thread, create_deluxe_train, create_transport_subtype
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from stationschedule.views import station_schedule

from travel.rasp.morda_backend.morda_backend.station.data_layer.train import TrainStationThread, TrainStationForPage
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_context import StationPageContext
from travel.rasp.morda_backend.morda_backend.station.data_layer.page_type import StationPageType, get_station_by_id


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True}, t_type=TransportType.TRAIN_ID)
create_station = create_station.mutate(t_type=TransportType.TRAIN_ID)


@replace_now('2020-02-01')
def test_train_station_thread():
    station1 = create_station(id=301, t_type=TransportType.TRAIN_ID)
    station2 = create_station(id=302, t_type=TransportType.TRAIN_ID)

    create_deluxe_train(numbers='333/444', title_ru='Самый фирменный', deluxe=True)
    DeLuxeTrain._number2deluxe_train = {}

    create_thread(
        canonical_uid='canonicalUid',
        title='Станция1 - Станция2',
        number='333',
        year_days=[date(2020, 1, 29), date(2020, 2, 1), date(2020, 2, 3)],
        schedule_v1=[[None, 0, station1, {'platform': 'пл.3 путь 5'}], [60, None, station2]]
    )

    schedule = station_schedule(station1, event='departure', t_type_code='train')
    route = list(schedule.schedule_routes)[0]
    st_for_page = mock.Mock(
        page_context=mock.Mock(event='departure', is_all_days=True),
        language='ru', country='ru'
    )
    thread = TrainStationThread(route, st_for_page)

    assert thread.canonical_uid == 'canonicalUid'
    assert thread.departure_from.isoformat() == '2020-02-01T00:00:00+03:00'
    assert thread.event_date_and_time['time'] == '00:00'
    assert thread.t_type == 'train'
    assert thread.title == 'Станция1 - Станция2'
    assert thread.days_text == 'только 29 января, 1, 3 февраля'
    assert thread.run_days_text == 'только 29 января, 1, 3 февраля'
    assert not hasattr(thread, 'except_days_text')
    assert not hasattr(thread, 'transport_subtype')
    assert not hasattr(thread, 'company')

    assert thread.platform == 'пл.3 путь 5'
    assert thread.deluxe_train_title == 'фирменный «Самый фирменный»'


@replace_now('2020-02-01')
def test_train_load_threads():
    ekb_tz = pytz.timezone('Etc/GMT-5')
    station0 = create_station(id=400, time_zone=ekb_tz)
    station1 = create_station(id=401, type_choices='train', time_zone=ekb_tz)
    station2 = create_station(id=402, time_zone=ekb_tz)

    create_deluxe_train(numbers='long/to', title_ru='Далеко')
    DeLuxeTrain._number2deluxe_train = {}
    from_subtype = create_transport_subtype(t_type=TransportType.TRAIN_ID, code='from', title_ru='Издалека')
    to_subtype = create_transport_subtype(t_type=TransportType.TRAIN_ID, code='to', title_ru='Далеко')

    create_thread(
        canonical_uid='cLong',
        title='Станция0 - Станция2',
        number='long',
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(1, 0),
        schedule_v1=[
            [None, 0, station0],
            [60, 70, station1, {'platform': '1', 'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cOther',
        title='Станция0 - Станция2',
        number='other',
        year_days=[date(2020, 2, 2)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(2, 0),
        schedule_v1=[
            [None, 0, station0],
            [70, 80, station1, {'time_zone': ekb_tz, 'train_number': '123E'}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cNext',
        title='Станция0 - Станция2',
        number='next',
        year_days=[date(2020, 2, 2)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(3, 0),
        schedule_v1=[
            [None, 0, station0],
            [70, 80, station1, {'time_zone': ekb_tz}],
            [120, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cTo',
        title='Станция1 - Станция2',
        number='to',
        t_subtype=to_subtype,
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(10, 0),
        schedule_v1=[
            [None, 0, station1, {'platform': '2', 'time_zone': ekb_tz}],
            [60, None, station2]
        ]
    )

    create_thread(
        canonical_uid='cFrom',
        title='Станция0 - Станция1',
        number='from',
        t_subtype=from_subtype,
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(5, 0),
        schedule_v1=[
            [None, 0, station0],
            [60, None, station1, {'platform': '3', 'time_zone': ekb_tz}]
        ]
    )

    create_thread(
        canonical_uid='cExpress',
        title='Станция0 - Станция2',
        year_days=[date(2020, 2, 1)],
        time_zone='Etc/GMT-3',
        tz_start_time=time(12, 0),
        schedule_v1=[[None, 0, station0], [120, None, station2]]
    )

    # Один день, отправление

    threads = _make_threads(station1, '2020-02-01', 'departure')

    assert len(threads) == 3
    assert threads[0].canonical_uid == 'cLong'
    assert threads[0].title == 'Станция0 - Станция2'
    assert threads[0].number == 'long'
    assert threads[0].t_type == 'train'
    assert threads[0].platform == '1'
    assert threads[0].deluxe_train_title == '«Далеко»'
    assert not hasattr(threads[0], 'transport_subtype')
    assert threads[0].departure_from.isoformat() == '2020-02-01T02:10:00+05:00'
    assert threads[0].event_date_and_time['time'] == '02:10'
    assert threads[0].event_date_and_time['datetime'].isoformat() == '2020-02-01T02:10:00+05:00'

    assert threads[1].canonical_uid == 'cTo'
    assert threads[1].title == 'Станция1 - Станция2'
    assert threads[1].number == 'to'
    assert threads[1].t_type == 'train'
    assert threads[1].platform == '2'
    assert threads[1].deluxe_train_title == '«Далеко»'
    assert_that(threads[1].transport_subtype, has_entries({
        'code': 'to',
        'title': 'Далеко'
    }))
    assert threads[1].departure_from.isoformat() == '2020-02-01T10:00:00+05:00'
    assert threads[1].event_date_and_time['time'] == '10:00'
    assert threads[1].event_date_and_time['datetime'].isoformat() == '2020-02-01T10:00:00+05:00'

    assert threads[2].canonical_uid == 'cOther'
    assert threads[2].title == 'Станция0 - Станция2'
    assert threads[2].number == '123E'
    assert threads[2].t_type == 'train'
    assert threads[2].platform == ''
    assert not hasattr(threads[2], 'deluxe_train_title')
    assert not hasattr(threads[2], 'transport_subtype')
    assert threads[2].departure_from.isoformat() == '2020-02-02T03:20:00+05:00'
    assert threads[2].event_date_and_time['time'] == '03:20'
    assert threads[2].event_date_and_time['datetime'].isoformat() == '2020-02-02T03:20:00+05:00'

    threads = _make_threads(station1, '2020-02-02', 'departure')

    assert len(threads) == 2

    assert threads[0].canonical_uid == 'cOther'
    assert threads[0].title == 'Станция0 - Станция2'
    assert threads[0].number == '123E'
    assert threads[0].t_type == 'train'
    assert threads[0].platform == ''
    assert not hasattr(threads[0], 'deluxe_train_title')
    assert not hasattr(threads[0], 'transport_subtype')
    assert threads[0].departure_from.isoformat() == '2020-02-02T03:20:00+05:00'
    assert threads[0].event_date_and_time['time'] == '03:20'
    assert threads[0].event_date_and_time['datetime'].isoformat() == '2020-02-02T03:20:00+05:00'

    assert threads[1].canonical_uid == 'cNext'
    assert threads[1].title == 'Станция0 - Станция2'
    assert threads[1].number == 'next'
    assert threads[1].t_type == 'train'
    assert threads[1].platform == ''
    assert not hasattr(threads[1], 'deluxe_train_title')
    assert not hasattr(threads[1], 'transport_subtype')
    assert threads[1].departure_from.isoformat() == '2020-02-02T04:20:00+05:00'
    assert threads[1].event_date_and_time['time'] == '04:20'
    assert threads[1].event_date_and_time['datetime'].isoformat() == '2020-02-02T04:20:00+05:00'

    threads = _make_threads(station1, '2020-02-03', 'departure')

    assert len(threads) == 0

    # Один день, прибытие

    threads = _make_threads(station1, '2020-02-01', 'arrival')

    assert len(threads) == 3
    assert threads[0].canonical_uid == 'cLong'
    assert threads[0].title == 'Станция0 - Станция2'
    assert threads[0].number == 'long'
    assert threads[0].t_type == 'train'
    assert threads[0].platform == '1'
    assert threads[0].deluxe_train_title == '«Далеко»'
    assert not hasattr(threads[0], 'transport_subtype')
    assert threads[0].departure == date(2020, 2, 1)
    assert threads[0].event_date_and_time['time'] == '02:00'
    assert threads[0].event_date_and_time['datetime'].isoformat() == '2020-02-01T02:00:00+05:00'

    assert threads[1].canonical_uid == 'cFrom'
    assert threads[1].title == 'Станция0 - Станция1'
    assert threads[1].number == 'from'
    assert threads[1].t_type == 'train'
    assert threads[1].platform == '3'
    assert not hasattr(threads[1], 'deluxe_train_title')
    assert_that(threads[1].transport_subtype, has_entries({
        'code': 'from',
        'title': 'Издалека'
    }))
    assert threads[1].departure == date(2020, 2, 1)
    assert threads[1].event_date_and_time['time'] == '06:00'
    assert threads[1].event_date_and_time['datetime'].isoformat() == '2020-02-01T06:00:00+05:00'

    assert threads[2].canonical_uid == 'cOther'
    assert threads[2].title == 'Станция0 - Станция2'
    assert threads[2].t_type == 'train'
    assert threads[2].platform == ''
    assert not hasattr(threads[2], 'deluxe_train_title')
    assert not hasattr(threads[2], 'transport_subtype')
    assert threads[2].departure == date(2020, 2, 2)
    assert threads[2].event_date_and_time['time'] == '03:10'
    assert threads[2].event_date_and_time['datetime'].isoformat() == '2020-02-02T03:10:00+05:00'

    threads = _make_threads(station1, '2020-02-02', 'arrival')

    assert len(threads) == 2

    assert threads[0].canonical_uid == 'cOther'
    assert threads[0].title == 'Станция0 - Станция2'
    assert threads[0].t_type == 'train'
    assert threads[0].platform == ''
    assert not hasattr(threads[0], 'deluxe_train_title')
    assert not hasattr(threads[0], 'transport_subtype')
    assert threads[0].departure == date(2020, 2, 2)
    assert threads[0].event_date_and_time['time'] == '03:10'
    assert threads[0].event_date_and_time['datetime'].isoformat() == '2020-02-02T03:10:00+05:00'

    assert threads[1].canonical_uid == 'cNext'
    assert threads[1].title == 'Станция0 - Станция2'
    assert threads[1].t_type == 'train'
    assert threads[1].platform == ''
    assert not hasattr(threads[1], 'deluxe_train_title')
    assert not hasattr(threads[1], 'transport_subtype')
    assert threads[1].departure == date(2020, 2, 2)
    assert threads[1].event_date_and_time['time'] == '04:10'
    assert threads[1].event_date_and_time['datetime'].isoformat() == '2020-02-02T04:10:00+05:00'

    threads = _make_threads(station1, '2020-02-03', 'arrival')

    assert len(threads) == 0

    # Все дни, отправление

    threads = _make_threads(station1, 'all-days', 'departure')

    assert len(threads) == 4
    assert threads[0].canonical_uid == 'cLong'
    assert threads[0].title == 'Станция0 - Станция2'
    assert threads[0].number == 'long'
    assert threads[0].t_type == 'train'
    assert threads[0].platform == '1'
    assert threads[0].deluxe_train_title == '«Далеко»'
    assert not hasattr(threads[0], 'transport_subtype')
    assert threads[0].departure_from.isoformat() == '2020-02-01T02:10:00+05:00'
    assert threads[0].event_date_and_time['time'] == '02:10'
    assert threads[0].days_text == 'только 1 февраля'
    assert threads[0].run_days_text == 'только 1 февраля'
    assert not hasattr(threads[0], 'except_days_text')

    assert threads[1].canonical_uid == 'cOther'
    assert threads[1].title == 'Станция0 - Станция2'
    assert threads[1].number == '123E'
    assert threads[1].t_type == 'train'
    assert threads[1].platform == ''
    assert not hasattr(threads[1], 'deluxe_train_title')
    assert not hasattr(threads[1], 'transport_subtype')
    assert threads[1].departure_from.isoformat() == '2020-02-02T03:20:00+05:00'
    assert threads[1].event_date_and_time['time'] == '03:20'
    assert threads[1].days_text == 'только 2 февраля'
    assert threads[1].run_days_text == 'только 2 февраля'
    assert not hasattr(threads[1], 'except_days_text')

    assert threads[2].canonical_uid == 'cNext'
    assert threads[2].title == 'Станция0 - Станция2'
    assert threads[2].number == 'next'
    assert threads[2].t_type == 'train'
    assert threads[2].platform == ''
    assert not hasattr(threads[2], 'deluxe_train_title')
    assert not hasattr(threads[2], 'transport_subtype')
    assert threads[2].departure_from.isoformat() == '2020-02-02T04:20:00+05:00'
    assert threads[2].event_date_and_time['time'] == '04:20'
    assert threads[2].days_text == 'только 2 февраля'
    assert threads[2].run_days_text == 'только 2 февраля'
    assert not hasattr(threads[2], 'except_days_text')

    assert threads[3].canonical_uid == 'cTo'
    assert threads[3].title == 'Станция1 - Станция2'
    assert threads[3].number == 'to'
    assert threads[3].t_type == 'train'
    assert threads[3].platform == '2'
    assert threads[3].deluxe_train_title == '«Далеко»'
    assert_that(threads[3].transport_subtype, has_entries({
        'code': 'to',
        'title': 'Далеко'
    }))
    assert threads[3].departure_from.isoformat() == '2020-02-01T10:00:00+05:00'
    assert threads[3].event_date_and_time['time'] == '10:00'
    assert threads[3].days_text == 'только 1 февраля'
    assert threads[3].run_days_text == 'только 1 февраля'
    assert not hasattr(threads[3], 'except_days_text')

    # Все дни, прибытие

    threads = _make_threads(station1, 'all-days', 'arrival')

    assert len(threads) == 4

    assert threads[0].canonical_uid == 'cLong'
    assert threads[0].title == 'Станция0 - Станция2'
    assert threads[0].number == 'long'
    assert threads[0].t_type == 'train'
    assert threads[0].platform == '1'
    assert threads[0].deluxe_train_title == '«Далеко»'
    assert not hasattr(threads[0], 'transport_subtype')
    assert threads[0].departure == date(2020, 2, 1)
    assert threads[0].event_date_and_time['time'] == '02:00'
    assert threads[0].days_text == 'только 1 февраля'
    assert threads[0].run_days_text == 'только 1 февраля'
    assert not hasattr(threads[0], 'except_days_text')

    assert threads[1].canonical_uid == 'cOther'
    assert threads[1].title == 'Станция0 - Станция2'
    assert threads[1].t_type == 'train'
    assert threads[1].platform == ''
    assert not hasattr(threads[1], 'deluxe_train_title')
    assert not hasattr(threads[1], 'transport_subtype')
    assert threads[1].departure == date(2020, 2, 2)
    assert threads[1].event_date_and_time['time'] == '03:10'
    assert threads[1].days_text == 'только 2 февраля'
    assert threads[1].run_days_text == 'только 2 февраля'
    assert not hasattr(threads[1], 'except_days_text')

    assert threads[2].canonical_uid == 'cNext'
    assert threads[2].title == 'Станция0 - Станция2'
    assert threads[2].t_type == 'train'
    assert threads[2].platform == ''
    assert not hasattr(threads[2], 'deluxe_train_title')
    assert not hasattr(threads[2], 'transport_subtype')
    assert threads[2].departure == date(2020, 2, 2)
    assert threads[2].event_date_and_time['time'] == '04:10'
    assert threads[2].days_text == 'только 2 февраля'
    assert threads[2].run_days_text == 'только 2 февраля'
    assert not hasattr(threads[2], 'except_days_text')

    assert threads[3].canonical_uid == 'cFrom'
    assert threads[3].title == 'Станция0 - Станция1'
    assert threads[3].number == 'from'
    assert threads[3].t_type == 'train'
    assert threads[3].platform == '3'
    assert not hasattr(threads[3], 'deluxe_train_title')
    assert_that(threads[3].transport_subtype, has_entries({
        'code': 'from',
        'title': 'Издалека'
    }))
    assert threads[3].departure == date(2020, 2, 1)
    assert threads[3].event_date_and_time['time'] == '06:00'
    assert threads[3].days_text == 'только 1 февраля'
    assert threads[3].run_days_text == 'только 1 февраля'
    assert not hasattr(threads[3], 'except_days_text')


def _make_threads(station, date, event):
    page_type = StationPageType(get_station_by_id(401), 'train')
    page_context = StationPageContext(station, date, event, environment.now_aware())
    st_for_page = TrainStationForPage(page_type, 'ru')
    st_for_page.load_threads(page_context)
    return st_for_page.threads
