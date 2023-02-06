# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date
from functools import partial
from urllib import quote_plus

import hamcrest
import pytest

from common.data_api.baris.helpers import BarisData
from common.data_api.baris.service import BarisResponse
from common.models.geo import StationTerminal, StationType
from common.tester.factories import create_company, create_direction, create_station, create_settlement
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.proxy_api.lib.station.models import (
    PlaneSegment, SuburbanDirection, SuburbanSegment, StationData
)
from travel.rasp.wizards.proxy_api.lib.station.formats import (
    format_plane_station, format_suburban_station, get_plane_station_urls, get_suburban_station_urls
)
from travel.rasp.wizards.proxy_api.lib.tests_utils import make_plane_station_query, make_segment, make_suburban_station_query
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType
from travel.rasp.wizards.wizard_lib.station.direction_type import DirectionType
from travel.rasp.wizards.wizard_lib.tests_utils import make_baris_flight


make_suburban_segment = partial(make_segment, factory=SuburbanSegment)
make_plane_segment = partial(make_segment, factory=PlaneSegment)


@pytest.mark.dbuser
@replace_now('2000-01-01')
@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_format_suburban_station():
    station = create_station(title='some_station', t_type='suburban', station_type=StationType.STATION_ID)
    other_station = create_station(title='other_station')
    create_direction(code='some_dir', title='Направление')
    query = make_suburban_station_query(station)
    data = StationData(directions=(
        SuburbanDirection(type=DirectionType.SUBDIR, code='на Юг', total=10, segments=(
            make_suburban_segment(
                departure_station=station,
                arrival_station=other_station,
                platform='platform_text',
                thread=dict(
                    express_type=ThreadExpressType.EXPRESS,
                    number='subdir_number',
                    start_date=date(2000, 1, 1),
                    title='subdir_title',
                )
            ),
        )),
        SuburbanDirection(type=DirectionType.ARRIVAL, code='arrival', total=10, segments=(
            make_suburban_segment(
                departure_station=other_station,
                arrival_station=station,
                stops='stops_text',
                thread=dict(
                    number='arrival_number',
                    start_date=date(2000, 1, 1),
                    title='arrival_title',
                ),
            ),
        )),
        SuburbanDirection(type=DirectionType.DEPARTURE, code='departure', total=10, segments=(
            make_suburban_segment(
                departure_station=station,
                arrival_station=other_station,
                thread=dict(
                    express_type=ThreadExpressType.AEROEXPRESS,
                    number='departure_number',
                    start_date=date(2000, 1, 1),
                    title='departure_title',
                ),
            ),
        )),
        SuburbanDirection(type=DirectionType.DIR, code='some_dir', total=10, segments=(
            make_suburban_segment(
                departure_station=station,
                arrival_station=other_station,
                thread=dict(
                    number='dir_number',
                    start_date=date(2000, 1, 1),
                    title='dir_title',
                ),
            ),
        )),
        SuburbanDirection(type=DirectionType.ALL, code='all', total=50, segments=(
            make_suburban_segment(
                departure_station=station,
                arrival_station=other_station,
                thread=dict(
                    number='all_number',
                    start_date=date(2000, 1, 1),
                    title='all_title',
                ),
            ),
        )),
    ))
    expected_desktop_url = (
        'https://rasp.yandex.ru/station/{}/?from=wraspstation&type=suburban&span=day&direction={{}}'.format(station.id)
    )
    expected_mobile_url = (
        'https://t.rasp.yandex.ru/station/{}/suburban/?from=wraspstation&filter=all&direction={{}}'.format(station.id)
    )

    assert format_suburban_station(data, query) == {
        'content': [
            {
                'segments': (
                    {
                        'arrival': '2000-01-01 12:00:00 +0000',
                        'arrival_station': 'other_station',
                        'departure': '2000-01-01 01:00:00 +0000',
                        'departure_station': 'some_station',
                        'duration': 660.0,
                        'express': True,
                        'from_station': 'от some_station',
                        'to_station': 'до other_station',
                        'number': 'subdir_number',
                        'platform': 'platform_text',
                        'stops': None,
                        'title': 'subdir_title',
                        'touch_url': 'http://mobile?from=wraspstation',
                        'transport_subtype_title': None,
                        'url': 'http://desktop?from=wraspstation'
                    },
                ),
                'title': 'на Юг',
                'total': 10,
                'touch_url': expected_mobile_url.format(quote_plus(b'на Юг')),
                'type': 'departure',
                'url': expected_desktop_url.format(quote_plus(b'на Юг'))
            },
            {
                'segments': (
                    {
                        'aeroexpress': True,
                        'arrival': '2000-01-01 12:00:00 +0000',
                        'arrival_station': 'other_station',
                        'departure': '2000-01-01 01:00:00 +0000',
                        'departure_station': 'some_station',
                        'duration': 660.0,
                        'from_station': 'от some_station',
                        'to_station': 'до other_station',
                        'number': 'departure_number',
                        'platform': None,
                        'stops': None,
                        'title': 'departure_title',
                        'touch_url': 'http://mobile?from=wraspstation',
                        'transport_subtype_title': None,
                        'url': 'http://desktop?from=wraspstation'
                    },
                ),
                'title': 'отправление',
                'total': 10,
                'touch_url': expected_mobile_url.format('departure'),
                'type': 'departure',
                'url': expected_desktop_url.format('departure')
            },
            {
                'segments': (
                    {
                        'arrival': '2000-01-01 12:00:00 +0000',
                        'arrival_station': 'other_station',
                        'departure': '2000-01-01 01:00:00 +0000',
                        'departure_station': 'some_station',
                        'duration': 660.0,
                        'from_station': 'от some_station',
                        'to_station': 'до other_station',
                        'number': 'dir_number',
                        'platform': None,
                        'stops': None,
                        'title': 'dir_title',
                        'touch_url': 'http://mobile?from=wraspstation',
                        'transport_subtype_title': None,
                        'url': 'http://desktop?from=wraspstation'
                    },
                ),
                'title': 'Направление',
                'total': 10,
                'touch_url': expected_mobile_url.format('some_dir'),
                'type': 'departure',
                'url': expected_desktop_url.format('some_dir')
            },
            {
                'segments': None,
                'title': 'все направления',
                'total': 50,
                'touch_url': expected_mobile_url.format('all'),
                'type': 'all',
                'url': expected_desktop_url.format('all')
            },
        ],
        'path_items': [
            {
                'text': 'rasp.yandex.ru',
                'touch_url': expected_mobile_url.format('all'),
                'url': expected_desktop_url.format('all')
            }
        ],
        'snippet': {
            '__hl':
                'Расписание электричек станции some_station. '
                'Дни курсирования, изменения, отмены, задержки, остановки на станциях.'
        },
        'title': {
            '__hl': 'Станция some_station: расписание электричек'
        },
        'touch_url': expected_mobile_url.format('all'),
        'type': 'suburban_directions',
        'url': expected_desktop_url.format('all')
    }


@pytest.mark.dbuser
def test_format_suburban_station_all_segments_hiding():
    station = create_station()
    other_station = create_station()
    data = StationData(directions=(
        SuburbanDirection(type=DirectionType.ALL, code='на Юг', total=10, segments=(
            make_suburban_segment(departure_station=station, arrival_station=other_station),
        )),
    ))

    hamcrest.assert_that(
        format_suburban_station(data, make_suburban_station_query(station)),
        hamcrest.has_entry(
            'content',
            hamcrest.contains(hamcrest.has_entries({'type': 'all', 'segments': None}))
        )
    )

    hamcrest.assert_that(
        format_suburban_station(data, make_suburban_station_query(
            station, experiment_flags=frozenset([ExperimentFlag.SUBURBAN_STATION_ALL_DIRECTION_SEGMENTS])
        )),
        hamcrest.has_entry(
            'content',
            hamcrest.contains(hamcrest.has_entries({'type': 'all', 'segments': hamcrest.has_length(1)}))
        )
    )


@pytest.mark.dbuser
def test_format_suburban_station_arrival_hiding():
    station = create_station()
    query = make_suburban_station_query(station)
    data = StationData(directions=(
        SuburbanDirection(type=DirectionType.SUBDIR, code='на Юг', total=10, segments=()),
        SuburbanDirection(type=DirectionType.ARRIVAL, code='arrival', total=10, segments=()),
    ))

    hamcrest.assert_that(format_suburban_station(data, query), hamcrest.has_entry(
        'content', hamcrest.contains(hamcrest.has_entry('title', 'на Юг'))
    ))

    data = StationData(directions=(
        SuburbanDirection(type=DirectionType.ARRIVAL, code='arrival', total=10, segments=()),
    ))

    hamcrest.assert_that(format_suburban_station(data, query), hamcrest.has_entry(
        'content', hamcrest.contains(hamcrest.has_entry('title', 'прибытие'))
    ))


@pytest.mark.dbuser
@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
@pytest.mark.parametrize('query_kwargs, expected_desktop_params, expected_mobile_params', (
    (
        dict(),
        'type=suburban&span=day',
        'filter=all'
    ),
    (
        dict(event_date=date(2000, 2, 1)),
        'type=suburban&span=schedule',
        'filter=all'
    ),
    (
        dict(experiment_flags=frozenset([ExperimentFlag.SUBURBAN_STATION_TOUCH_TODAY_URL])),
        'type=suburban&span=day',
        'filter=today'
    ),
))
def test_get_suburban_station_urls(query_kwargs, expected_desktop_params, expected_mobile_params):
    station = create_station()
    query = make_suburban_station_query(station, **query_kwargs)

    assert get_suburban_station_urls('direction_code', query) == {
        'touch_url':
            'https://t.rasp.yandex.ru/station/{}/suburban/?from=wraspstation&{}&direction=direction_code'
            .format(station.id, expected_mobile_params),
        'url':
            'https://rasp.yandex.ru/station/{}/?from=wraspstation&{}&direction=direction_code'
            .format(station.id, expected_desktop_params)
    }


@pytest.mark.dbuser
@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
@pytest.mark.parametrize('station_tablo_state, direction_type, event_date, expected_params', (
    ('', None, None, '&span=day'),
    ('', None, date(2000, 2, 1), '&span=schedule'),
    ('real', None, None, ''),
    ('real', DirectionType.DEPARTURE, None, '&event=departure'),
    ('', DirectionType.DEPARTURE, None, '&span=day&event=departure'),
))
def test_get_plane_station_urls(station_tablo_state, direction_type, event_date, expected_params):
    station = create_station(tablo_state=station_tablo_state)
    query = make_plane_station_query(station, event_date=event_date)

    assert get_plane_station_urls(query, direction_type) == {
        'touch_url': 'https://t.rasp.yandex.ru/station/{}/?from=wraspstatus{}'.format(station.id, expected_params),
        'url': 'https://rasp.yandex.ru/station/{}/?from=wraspstatus{}'.format(station.id, expected_params)
    }


def build_baris_data(flight_params_list):
    flights = []
    stations_ids = []
    companies_ids = []
    for flight_params in flight_params_list:
        station_from = flight_params['from']
        station_to = flight_params['to']

        flight = make_baris_flight(station_from, station_to, flight_params)
        flight.update({
            'route': [station_from.id, station_to.id],
            'stationFrom': station_from.id,
            'stationTo': station_to.id,
            'source': ''
        })

        if 'airlineID' in flight_params and flight_params['airlineID'] not in companies_ids:
            companies_ids.append(flight_params['airlineID'])

        if station_from.id not in companies_ids:
            stations_ids.append(station_from.id)
        if station_to.id not in companies_ids:
            stations_ids.append(station_to.id)

        flights.append(flight)

    baris_data = BarisData(BarisResponse(
        flights,
        stations_ids=stations_ids,
        companies_ids=companies_ids,
        models_ids=[]
    ))

    return baris_data


@pytest.mark.dbuser
@replace_now('2000-01-01')
@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_format_plane_station_departure():
    settlement = create_settlement(title='some_settlement', time_zone='Etc/GMT-3')
    station = create_station(
        title='some_station',
        settlement=settlement,
        t_type='plane',
        station_type=StationType.STATION_ID
    )
    other_settlement = create_settlement(title='other_settlement', time_zone='Etc/GMT-3')
    other_station = create_station(title='other_station', settlement=other_settlement)
    company = create_company(short_title_ru='some_company')
    terminal = StationTerminal.objects.create(station=station, name='X')

    query = make_plane_station_query(station, direction_type=DirectionType.DEPARTURE)
    baris_data = build_baris_data([{
        'from': station,
        'to': other_station,
        'airlineID': company.id,
        'number': 'departure_number',
        'title': 'departure title',
        'datetime': '2000-01-01T00:00:00+03:00',
        'status': {
            'status': 'on_time',
            'departure': '2000-01-01 01:30:00',
            'departureStatus': 'on_time',
            'departureGate': 'G21',
            'departureTerminal': terminal.name,
            'createdAtUtc': '2000-01-01 01:00:00',
            'updatedAtUtc': '2000-01-01 01:00:00',
            'departureUpdatedAtUtc': '2000-01-01 01:00:00',
        }
    }])

    expected_desktop_url = 'https://rasp.yandex.ru/station/{}/?from=wraspstatus&span=day&event={{}}'.format(station.id)
    expected_mobile_url = 'https://t.rasp.yandex.ru/station/{}/?from=wraspstatus&span=day&event={{}}'.format(station.id)

    expected_station_mobile_url = 'https://t.rasp.yandex.ru/station/{}/?from=wraspstatus&span=day'.format(station.id)
    expected_station_desktop_url = 'https://rasp.yandex.ru/station/{}/?from=wraspstatus&span=day'.format(station.id)

    expected_segment_url = b'https://travel-test.yandex.ru/avia/flights/departure-title/?lang=ru'

    assert format_plane_station(baris_data, query) == {
        'content': [
            {
                'segments': [
                    {
                        'arrival_settlement': 'other_settlement',
                        'departure_settlement': 'some_settlement',
                        'company': 'some_company',
                        'expected': b'2000-01-01 01:30:00 +0300',
                        'gate': 'G21',
                        'number': 'departure title',
                        'scheduled': b'2000-01-01 00:00:00 +0300',
                        'status': 'ожидается',
                        'terminal': 'X',
                        'title': 'some_settlement \N{em dash} other_settlement',
                        'touch_url': expected_segment_url,
                        'url': expected_segment_url
                    }
                ],
                'selected': True,
                'title': 'Вылет',
                'touch_url': expected_mobile_url.format('departure'),
                'type': 'departure',
                'url': expected_desktop_url.format('departure')
            },
            {
                'segments': [],
                'selected': False,
                'title': 'Прилёт',
                'touch_url': expected_mobile_url.format('arrival'),
                'type': 'arrival',
                'url': expected_desktop_url.format('arrival')
            }
        ],
        'path_items': [
            {
                'text': 'Яндекс.Расписания',
                'touch_url': 'https://t.rasp.yandex.ru/?from=wraspstatus',
                'url': 'https://rasp.yandex.ru/?from=wraspstatus',
            },
            {
                'text': 'some_station',
                'touch_url': expected_station_mobile_url,
                'url': expected_station_desktop_url
            },
            {
                'text': 'Табло вылета',
                'touch_url': expected_mobile_url.format('departure'),
                'url': expected_desktop_url.format('departure')
            },
        ],
        'snippet': {
            '__hl':
                'Актуальное расписание прилётов и\xa0вылетов из\xa0станции some_station.'
        },
        'title': {
            '__hl': 'Табло вылета станции some_station'
        },
        'touch_url': expected_mobile_url.format('departure'),
        'type': 'airport_panel_with_event',
        'url': expected_desktop_url.format('departure'),
        'search_props': {
            'plane-connection': 1,
        }
    }


@pytest.mark.dbuser
@replace_now('2000-01-01')
@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_format_plane_station_arrival():
    settlement = create_settlement(title='some_settlement', time_zone='Etc/GMT-3')
    station = create_station(
        title='some_station',
        settlement=settlement,
        t_type='plane',
        station_type=StationType.STATION_ID
    )
    other_settlement = create_settlement(title='other_settlement', time_zone='Etc/GMT-3')
    other_station = create_station(title='other_station', settlement=other_settlement)
    company = create_company(short_title_ru='some_company')
    terminal = StationTerminal.objects.create(station=station, name='X')

    query = make_plane_station_query(station, direction_type=DirectionType.ARRIVAL)
    baris_data = build_baris_data([{
        'from': other_station,
        'to': station,
        'airlineID': company.id,
        'number': 'arrival_number',
        'title': 'arrival title',
        'datetime': '2000-01-01T00:00:00+03:00',
        'status': {
            'status': 'on_time',
            'arrival': '2000-01-01 01:30:00',
            'arrivalStatus': 'on_time',
            'arrivalGate': 'G21',
            'arrivalTerminal': terminal.name,
            'createdAtUtc': '2000-01-01 01:00:00',
            'updatedAtUtc': '2000-01-01 01:00:00',
            'arrivalUpdatedAtUtc': '2000-01-01 01:00:00',
        }
    }])

    expected_desktop_url = 'https://rasp.yandex.ru/station/{}/?from=wraspstatus&span=day&event={{}}'.format(station.id)
    expected_mobile_url = 'https://t.rasp.yandex.ru/station/{}/?from=wraspstatus&span=day&event={{}}'.format(station.id)

    expected_station_mobile_url = 'https://t.rasp.yandex.ru/station/{}/?from=wraspstatus&span=day'.format(station.id)
    expected_station_desktop_url = 'https://rasp.yandex.ru/station/{}/?from=wraspstatus&span=day'.format(station.id)

    expected_segment_url = b'https://travel-test.yandex.ru/avia/flights/arrival-title/?lang=ru'

    assert format_plane_station(baris_data, query) == {
        'content': [
            {
                'segments': [],
                'selected': False,
                'title': 'Вылет',
                'touch_url': expected_mobile_url.format('departure'),
                'type': 'departure',
                'url': expected_desktop_url.format('departure')
            },
            {
                'segments': [
                    {
                        'arrival_settlement': 'some_settlement',
                        'departure_settlement': 'other_settlement',
                        'company': 'some_company',
                        'expected': b'2000-01-01 01:30:00 +0300',
                        'gate': 'G21',
                        'number': 'arrival title',
                        'scheduled': b'2000-01-01 00:00:00 +0300',
                        'status': 'ожидается',
                        'terminal': 'X',
                        'title': 'other_settlement \N{em dash} some_settlement',
                        'touch_url': expected_segment_url,
                        'url': expected_segment_url
                    }
                ],
                'selected': True,
                'title': 'Прилёт',
                'touch_url': expected_mobile_url.format('arrival'),
                'type': 'arrival',
                'url': expected_desktop_url.format('arrival')
            },
        ],
        'path_items': [
            {
                'text': 'Яндекс.Расписания',
                'touch_url': 'https://t.rasp.yandex.ru/?from=wraspstatus',
                'url': 'https://rasp.yandex.ru/?from=wraspstatus',
            },
            {
                'text': 'some_station',
                'touch_url': expected_station_mobile_url,
                'url': expected_station_desktop_url
            },
            {
                'text': 'Табло прилета',
                'touch_url': expected_mobile_url.format('arrival'),
                'url': expected_desktop_url.format('arrival')
            },
        ],
        'snippet': {
            '__hl':
                'Актуальное расписание прилётов и\xa0вылетов из\xa0станции some_station.'
        },
        'title': {
            '__hl': 'Табло прилёта станции some_station'
        },
        'touch_url': expected_mobile_url.format('arrival'),
        'type': 'airport_panel_with_event',
        'url': expected_desktop_url.format('arrival'),
        'search_props': {
            'plane-connection': 1,
        }
    }


@pytest.mark.dbuser
@replace_now('2000-01-01')
def test_format_plane_station_company():
    station = create_station(t_type='plane', station_type=StationType.STATION_ID)
    other_station = create_station()
    company_with_short_title = create_company(short_title_ru='short_company_title')
    company_with_long_title = create_company(short_title_ru='', title_ru='long_company_title')

    baris_data = build_baris_data([
        {
            'from': station,
            'to': other_station,
            'airlineID': None
        },
        {
            'from': station,
            'to': other_station,
            'airlineID': company_with_short_title.id
        },
        {
            'from': station,
            'to': other_station,
            'airlineID': company_with_long_title.id
        },
    ])

    hamcrest.assert_that(
        format_plane_station(baris_data, make_plane_station_query(station, direction_type=DirectionType.DEPARTURE)),
        hamcrest.has_entry('content', hamcrest.has_item(hamcrest.has_entry('segments', hamcrest.contains(
            hamcrest.not_(hamcrest.has_key('company')),
            hamcrest.has_entry('company', 'short_company_title'),
            hamcrest.has_entry('company', 'long_company_title'),
        ))))
    )


@pytest.mark.dbuser
@replace_now('2000-01-01')
@replace_setting('PLANE_STATION_MAXIMUM_SEGMENTS', 5)
def test_format_plane_station_event_update():
    station = create_station(t_type='plane', station_type=StationType.STATION_ID, time_zone='UTC')
    other_station = create_station()

    arrival_baris_data = build_baris_data([
        {
            'from': other_station,
            'to': station,
            'datetime': '2000-01-01T12:00:00+00:00',
            'status': {
                'arrival': '2000-01-01 12:30:00',
                'arrivalStatus': 'delayed',
                'arrivalTerminal': None,
                'arrivalGate': None,
                'checkInDesks': None,
                'baggageCarousels': 'carousel_name',
            }
        },
        {
            'from': other_station,
            'to': station,
            'datetime': '2000-01-01T14:00:00+00:00',
            'status': {
                'arrival': '2000-01-01 14:30:00',
                'arrivalStatus': 'arrived',
                'arrivalTerminal': 'arrived_terminal',
                'arrivalGate': 'arrival_gate',
                'checkInDesks': None,
            }
        },
        {
            'from': other_station,
            'to': station,
            'datetime': '2000-01-01T16:00:00+00:00',
            'status': {
                'arrival': '2000-01-01 15:30:00',
                'arrivalStatus': 'early',
                'arrivalTerminal': 'arrived_terminal',
                'arrivalGate': 'arrival_gate',
                'checkInDesks': None,
            }
        },
    ])

    hamcrest.assert_that(
        format_plane_station(
            arrival_baris_data,
            make_plane_station_query(station, direction_type=DirectionType.ARRIVAL)
        ),
        hamcrest.has_entry('content', hamcrest.contains_inanyorder(
            hamcrest.has_entries({
                'segments': [],
                'type': 'departure'
            }),
            hamcrest.has_entries({
                'segments': hamcrest.contains(
                    hamcrest.has_entries({
                        'expected': '2000-01-01 12:30:00 +0000',
                        'scheduled': '2000-01-01 12:00:00 +0000',
                        'status': 'опаздывает',
                        'baggage_carousels': 'carousel_name',
                    }),
                    hamcrest.has_entries({
                        'expected': '2000-01-01 14:30:00 +0000',
                        'scheduled': '2000-01-01 14:00:00 +0000',
                        'status': 'прибыл',
                        'terminal': 'arrived_terminal',
                        'gate': 'arrival_gate'
                    }),
                    hamcrest.has_entries({
                        'expected': '2000-01-01 15:30:00 +0000',
                        'scheduled': '2000-01-01 16:00:00 +0000',
                        'status': 'прилетит раньше',
                        'terminal': 'arrived_terminal',
                        'gate': 'arrival_gate'
                    })
                ),
                'type': 'arrival'
            }),
        ))
    )

    departure_baris_data = build_baris_data([
        {
            'from': station,
            'to': other_station,
            'datetime': '2000-01-01T01:00:00+00:00',
            'status': {
                'departure': '2000-01-01 01:30:00',
                'departureStatus': 'delayed',
                'departureTerminal': 'terminal_name',
                'departureGate': None,
                'checkInDesks': None,
                'baggageCarousels': None,
            }
        },
        {
            'from': station,
            'to': other_station,
            'datetime': '2000-01-01T01:00:00+00:00',
            'status': {
                'departure': '2000-01-01 01:00:00',
                'departureStatus': 'cancelled',
                'departureTerminal': None,
                'departureGate': None,
                'checkInDesks': None,
                'baggageCarousels': None,
            }
        },
        {
            'from': station,
            'to': other_station,
            'datetime': '2000-01-01T01:00:00+00:00',
            'status': {
                'departure': '2000-01-01 00:30:00',
                'departureStatus': 'early',
                'departureTerminal': None,
                'departureGate': 'G21',
                'checkInDesks': 'check_in_desk_name',
                'baggageCarousels': None,
            }
        },
        {
            'from': station,
            'to': other_station,
            'datetime': '2000-01-01T01:00:00+00:00',
            'status': {
                'departure': '2000-01-01 01:00:00',
                'departureStatus': 'on_time',
                'departureTerminal': None,
                'departureGate': None,
                'checkInDesks': None,
                'baggageCarousels': None,
            }
        },
        {
            'from': station,
            'to': other_station,
            'datetime': '2000-01-01T00:00:00+00:00',
            'status': {
                'departure': '2000-01-01 00:22:00',
                'departureStatus': 'departed',
                'departureTerminal': None,
                'departureGate': None,
                'checkInDesks': None,
                'baggageCarousels': None,
            }
        },
    ])

    hamcrest.assert_that(
        format_plane_station(
            departure_baris_data,
            make_plane_station_query(station, direction_type=DirectionType.DEPARTURE)
        ),
        hamcrest.has_entry('content', hamcrest.contains_inanyorder(
            hamcrest.has_entries({
                'segments': hamcrest.contains(
                    hamcrest.has_entries({
                        'expected': '2000-01-01 01:30:00 +0000',
                        'scheduled': '2000-01-01 01:00:00 +0000',
                        'terminal': 'terminal_name',
                        'status': 'задержан'
                    }),
                    hamcrest.has_entries({
                        'expected': '2000-01-01 01:00:00 +0000',
                        'scheduled': '2000-01-01 01:00:00 +0000',
                        'status': 'отменён'
                    }),
                    hamcrest.has_entries({
                        'expected': '2000-01-01 00:30:00 +0000',
                        'scheduled': '2000-01-01 01:00:00 +0000',
                        'status': 'вылет раньше',
                        'check_in_desks': 'check_in_desk_name',
                        'gate': 'G21',
                    }),
                    hamcrest.has_entries({
                        'expected': '2000-01-01 01:00:00 +0000',
                        'scheduled': '2000-01-01 01:00:00 +0000',
                        'status': 'ожидается'
                    }),
                    hamcrest.has_entries({
                        'expected': '2000-01-01 00:22:00 +0000',
                        'scheduled': '2000-01-01 00:00:00 +0000',
                        'status': 'вылетел'
                    }),
                ),
                'type': 'departure'
            }),
            hamcrest.has_entries({
                'segments': [],
                'type': 'arrival'
            }),
        ))
    )


@pytest.mark.dbuser
@replace_now('2000-01-01')
def test_format_plane_station_tablo_state_statuses():
    other_station = create_station()
    station = create_station(t_type='plane', station_type=StationType.STATION_ID, time_zone='UTC', tablo_state='')

    hamcrest.assert_that(
        format_plane_station(
            build_baris_data([{
                'from': station,
                'to': other_station,
                'datetime': '2000-01-01T01:00:00+00:00',
            }]),
            make_plane_station_query(station, direction_type=DirectionType.DEPARTURE)
        ),
        hamcrest.has_entry('content', hamcrest.has_item(hamcrest.has_entry('segments', hamcrest.contains(
            hamcrest.has_entries({
                'expected': '2000-01-01 01:00:00 +0000',
                'scheduled': '2000-01-01 01:00:00 +0000',
                'status': 'ожидается'
            }),
        ))))
    )

    station = create_station(t_type='plane', station_type=StationType.STATION_ID, time_zone='UTC', tablo_state='real')

    hamcrest.assert_that(
        format_plane_station(
            build_baris_data([{
                'from': station,
                'to': other_station,
                'datetime': '2000-01-01T01:00:00+00:00',
            }]),
            make_plane_station_query(station, direction_type=DirectionType.DEPARTURE)
        ),
        hamcrest.has_entry('content', hamcrest.has_item(hamcrest.has_entry('segments', hamcrest.contains(
            hamcrest.has_entries({
                'expected': '2000-01-01 01:00:00 +0000',
                'scheduled': '2000-01-01 01:00:00 +0000',
                'status': 'нет данных'
            }),
        ))))
    )

    station = create_station(t_type='plane', station_type=StationType.STATION_ID, time_zone='UTC', tablo_state='nodata')

    hamcrest.assert_that(
        format_plane_station(
            build_baris_data([{
                'from': station,
                'to': other_station,
                'datetime': '2000-01-01T01:00:00+00:00',
                'status': {
                    'status': 'delayed',
                    'departure': '2000-01-01 01:30:00',
                    'departureStatus': 'delayed',
                    'departureGate': None,
                    'departureTerminal': None,
                    'createdAtUtc': '2000-01-01 00:00:00',
                    'updatedAtUtc': '2000-01-01 00:00:00',
                    'departureUpdatedAtUtc': '2000-01-01 00:00:00',
                }
            }]),
            make_plane_station_query(station, direction_type=DirectionType.DEPARTURE)
        ),
        hamcrest.has_entry('content', hamcrest.has_item(hamcrest.has_entry('segments', hamcrest.contains(
            hamcrest.has_entries({
                'expected': '2000-01-01 01:30:00 +0000',
                'scheduled': '2000-01-01 01:00:00 +0000',
                'status': 'нет данных'
            }),
        ))))
    )
