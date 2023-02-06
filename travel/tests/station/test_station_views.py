# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date
from urllib import urlencode

import mock
import pytest
from django.test import Client
from hamcrest import has_entries, assert_that, contains

from common.data_api.baris.test_helpers import mock_baris_response
from common.data_api.search_stats.search_stats import search_stats
from common.models.geo import Country, CodeSystem, StationType
from common.models.transport import TransportType
from common.tester.factories import (
    create_settlement, create_station, create_thread, create_transport_subtype,
    create_station_terminal, create_company, create_station_code, create_way_to_airport
)
from common.tester.utils.datetime import replace_now
from common.models.schedule import RThread


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True})
create_station = create_station.mutate(country=Country.RUSSIA_ID)


def _get_response(request_name, query, lang='ru'):
    qs = urlencode(query)
    response = Client().get('/{lang}/{name}/?{qs}'.format(
        lang=lang,
        name=request_name,
        qs=qs
    ))
    return response.status_code, json.loads(response.content)


def test_station_quick_view():
    station1 = create_station(id=101, t_type=TransportType.TRAIN_ID, type_choices='tablo,suburban')
    station2 = create_station(id=102)
    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station1], [10, None, station2]]
    )

    code, response = _get_response('station/quick', {
        'station_id': 101, 'subtype': 'suburban'
    })

    assert code == 200
    assert_that(response['result'], has_entries({
        'type': 'train',
        'subtypes': ['suburban'],
        'mainSubtype': 'suburban',
        'currentSubtype': 'suburban',
        'notEnoughInfo': False
    }))

    code, response = _get_response('station/quick', {
        'station_id': 101, 'subtype': 'train'
    })

    assert code == 200
    assert_that(response['result'], has_entries({
        'type': 'train',
        'subtypes': ['suburban'],
        'mainSubtype': 'suburban',
        'currentSubtype': None,
        'notEnoughInfo': False
    }))

    station3 = create_station(id=103, t_type=TransportType.PLANE_ID, type_choices='tablo')
    create_station_terminal(id=1031, name='A', station=station3, is_domestic=True)
    create_station_terminal(id=1032, name='B', station=station3, is_international=True)

    code, response = _get_response('station/quick', {
        'station_id': 103, 'subtype': 'plane'
    })

    assert code == 200
    assert_that(response['result'], has_entries({
        'type': 'plane',
        'subtypes': ['plane'],
        'mainSubtype': 'plane',
        'currentSubtype': 'plane',
        'notEnoughInfo': False,
        'terminals': contains(
            has_entries({'id': 1031, 'name': 'A', 'isDomestic': True, 'isInternational': False}),
            has_entries({'id': 1032, 'name': 'B', 'isDomestic': False, 'isInternational': True})
        )
    }))


def test_station_quick_bad_response():
    station1 = create_station(id=101, t_type=TransportType.TRAIN_ID, type_choices='train,suburban')
    station2 = create_station(id=102)
    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station1], [10, None, station2]]
    )

    code, response = _get_response('station/quick', {})
    assert code == 400
    assert response['errors'] == {'station_id': ['station_id should be in the request params']}

    code, response = _get_response('station/quick', {
        'station_id': 100
    })
    assert code == 404
    assert response['errors'] == ['Станции с id 100 нет в базе']


@replace_now('2020-02-01')
def test_station_view():
    settlement = create_settlement(slug='city', title_ru='город')
    station1 = create_station(
        id=101, t_type=TransportType.TRAIN_ID, slug='station1',
        type_choices='train,suburban,tablo', settlement=settlement,
        popular_title_ru='станция', popular_title_ru_genitive='станции', address_ru='адрес',
        near_metro='метро', longitude=60.6, latitude=40.4, station_type=StationType.objects.get(id=StationType.BUS_STATION_ID)
    )
    station2 = create_station(id=102, slug='station2')

    t_subtype = create_transport_subtype(t_type=TransportType.SUBURBAN_ID, code='lastochka', title_ru='Ласточка')
    create_thread(
        t_type=TransportType.SUBURBAN_ID, canonical_uid='canonicalUid',
        title='электричка', number='111', comment='комментарий',
        express_type='aeroexpress', t_subtype=t_subtype,
        year_days=[date(2020, 1, 29), date(2020, 2, 1), date(2020, 2, 3)],
        schedule_v1=[[None, 0, station1, {'platform': 'пл1'}], [10, None, station2]]
    )

    create_way_to_airport(
        station_id=101,
        title_ru='аэроэкспресс', way_type='aeroexpress',
        from_station_id=101, to_station_id=102
    )

    code, response = _get_response('station', {
        'station_id': 101, 'subtype': 'suburban', 'event': 'departure', 'date': 'today'
    })

    assert code == 200
    assert_that(response['result'], has_entries({
        'pageType': has_entries({
            'type': 'train',
            'subtypes': ['train', 'suburban', 'tablo'],
            'mainSubtype': 'train',
            'currentSubtype': 'suburban',
            'notEnoughInfo': False
        }),
        'station': has_entries({
            'id': 101,
            'title': 'станция',
            'titleGenitive': 'станции',
            'fullTitle': 'станция',
            'fullTitleGenitive': 'станции',
            'fullTitleDative': 'станция',
            'hasPopularTitle': True,
            'longitude': 60.6,
            'latitude': 40.4,
            'subway': 'м. метро',
            'address': 'адрес',
            'settlement': has_entries({
                'slug': 'city',
                'title': 'город',
            }),
            'stationType': 'автовокзал',
            'wayToAirport': has_entries({
                'fromPointId': 's101',
                'toPointId': 's102',
                'fromSlug': 'station1',
                'toSlug': 'station2',
                'linkTitle': 'аэроэкспресс',
                'wayType': 'aeroexpress'
            })
        }),
        'context': has_entries({
            'event': 'departure',
            'when': has_entries({
                'special': 'today',
                'date': '2020-02-01',
            }),
            'dtNow': '2020-02-01T00:00:00+03:00'
        }),
        'threads': contains(has_entries({
            'canonicalUid': 'canonicalUid',
            'transportType': 'suburban',
            'title': 'электричка',
            'number': '111',
            'comment': 'комментарий',
            'departureFrom': '2020-02-01T00:00:00+03:00',
            'eventDt': {
                'datetime': '2020-02-01T00:00:00+03:00',
                'time': '00:00'
            },
            'isExpress': False,
            'isAeroExpress': True,
            'transportSubtype': {
                'code': 'lastochka',
                'title': 'Ласточка'
            },
            'platform': 'пл1',
        }))
    }))

    m_days_text = mock.Mock(side_effect=[
        {
            'days_text': 'run_days',
            'except_days_text': 'except_days'
        }
    ])
    with mock.patch.object(RThread, 'L_days_text_dict', m_days_text):
        code, response = _get_response('station', {
            'station_id': 101, 'subtype': 'suburban', 'date': 'all-days'
        })

        assert code == 200
        assert_that(response['result'], has_entries({
            'pageType': has_entries({
                'type': 'train',
                'subtypes': ['train', 'suburban', 'tablo'],
                'mainSubtype': 'train',
                'currentSubtype': 'suburban',
                'notEnoughInfo': False
            }),
            'station': has_entries({
                'id': 101,
                'title': 'станция',
                'titleGenitive': 'станции',
                'longitude': 60.6,
                'latitude': 40.4,
                'subway': 'м. метро',
                'address': 'адрес',
            }),
            'context': has_entries({
                'event': 'departure',
                'when': has_entries({
                    'special': 'all-days'
                }),
                'dtNow': '2020-02-01T00:00:00+03:00'
            }),
            'threads': contains(has_entries({
                'canonicalUid': 'canonicalUid',
                'transportType': 'suburban',
                'title': 'электричка',
                'number': '111',
                'comment': 'комментарий',
                'departureFrom': '2020-02-01T00:00:00+03:00',
                'eventDt': {
                    'time': '00:00'
                },
                'isExpress': False,
                'isAeroExpress': True,
                'transportSubtype': {
                    'code': 'lastochka',
                    'title': 'Ласточка'
                },
                'platform': 'пл1',
                'daysText': 'только 29 января, 1, 3 февраля',
                'runDaysText': 'run_days',
                'exceptDaysText': 'except_days'
            }))
        }))


BARIS_RESPONSE = {
    'direction': 'departure',
    'station': 200,
    'flights': [{
        'airlineID': 300,
        'number': '22',
        'title': 'PP 22',
        'datetime': '2020-02-01T02:00:00+03:00',
        'terminal': 'B',
        'codeshares': [{
            'airlineID': 300,
            'number': 33,
            'title': 'PP 33'
        }],
        'source': 'flight-board',
        'route': [200, 250],
        'status':{
            'departure': '2020-02-01 03:30:00',
            'departureStatus': 'delayed',
            'departureGate': '9',
            'departureTerminal': 'B',
            'checkInDesks': '7-8',
            'baggageCarousels': '',
            'diverted': True,
            'divertedAirportID': 200
        }
    }]
}


@replace_now('2020-02-01')
def test_airport_view():
    iata_code_system = CodeSystem.objects.get(code='iata')
    create_company(id=300, short_title='П', title='Полеты', url='url', svg_logo='icon.svg', hidden=False)

    settlement1 = create_settlement(title='Город1')
    station1 = create_station(
        id=200,
        title='Аэропорт1',
        t_type=TransportType.PLANE_ID,
        type_choices='tablo',
        settlement=settlement1,
        tablo_state='real'
    )
    create_station_code(station=station1, system=iata_code_system, code='XXX')
    create_station_terminal(id=201, name='A', station=station1, is_domestic=True)
    create_station_terminal(id=202, name='B', station=station1, is_international=True)

    settlement2 = create_settlement(title='Город2')
    station2 = create_station(
        id=250,
        title='Аэропорт2',
        t_type=TransportType.PLANE_ID,
        type_choices='tablo',
        settlement=settlement2
    )
    create_station_code(station=station2, system=iata_code_system, code='YYY')

    with mock_baris_response(BARIS_RESPONSE):
        code, response = _get_response('station', {'station_id': 200})

        assert code == 200
        assert_that(response['result'], has_entries({
            'pageType': has_entries({
                'type': 'plane',
                'subtypes': ['plane'],
                'mainSubtype': 'plane',
                'currentSubtype': 'plane',
                'notEnoughInfo': False,
                'terminals': contains(
                    has_entries({'id': 201, 'name': 'A', 'isDomestic': True, 'isInternational': False}),
                    has_entries({'id': 202, 'name': 'B', 'isDomestic': False, 'isInternational': True})
                )
            }),
            'station': has_entries({
                'id': 200,
                'trusted': True,
                'iataCode': 'XXX'
            }),
            'context': has_entries({
                'event': 'departure',
                'when': has_entries({
                    'special': 'today',
                    'date': '2020-02-01',
                    'dtAfter': '2020-02-01T00:00:00+03:00',
                    'dtBefore': '2020-02-02T00:00:00+03:00'
                }),
                'dtNow': '2020-02-01T00:00:00+03:00'
            }),
            'companies': contains(
                has_entries({
                    'shortTitle': 'П',
                    'title': 'Полеты',
                    'url': 'url',
                    'id': 300,
                    'icon': 'icon.svg'
                })
            ),
            'threads': contains(has_entries({
                'transportType': 'plane',
                'number': 'PP 22',
                'companyId': 300,
                'terminal': 'B',
                'eventDt': has_entries({
                    'time': '02:00',
                    'datetime': '2020-02-01T02:00:00+03:00'
                }),
                'isSupplement': True,
                'routeStations': contains(
                    {
                        'settlement': 'Город2',
                        'iataCode': 'YYY',
                        'title': 'Аэропорт2'
                    }
                ),
                'codeshares': contains(
                    {
                        'number': 'PP 33',
                        'companyId': 300
                    }
                ),
                'status': has_entries({
                    'status': 'delayed',
                    'actualDt': '2020-02-01T03:30:00+03:00',
                    'actualTerminal': 'B',
                    'checkInDesks': '7-8',
                    'gate': '9',
                    'baggageCarousels': '',
                    'diverted': has_entries({
                        'settlement': 'Город1',
                        'iataCode': 'XXX',
                        'title': 'Аэропорт1'
                    })
                }),
            }))
        }))


def test_station_bad_response():
    station1 = create_station(id=101, t_type=TransportType.TRAIN_ID, type_choices='train,suburban')
    station2 = create_station(id=102)
    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station1], [10, None, station2]]
    )

    code, response = _get_response('station', {})
    assert code == 400
    assert response['errors'] == {'station_id': ['station_id should be in the request params']}

    code, response = _get_response('station', {
        'station_id': 100
    })
    assert code == 404
    assert response['errors'] == ['Станции с id 100 нет в базе']

    code, response = _get_response('station', {
        'station_id': 101, 'date': 'yesterday'
    })
    assert code == 400
    assert response['errors'] == {'date': ['wrong date format']}

    code, response = _get_response('station', {
        'station_id': 101, 'date': '10.10.2020'
    })
    assert code == 400
    assert response['errors'] == {'date': ['wrong date format']}

    create_station(id=103, t_type=TransportType.PLANE_ID, type_choices='plane')

    code, response = _get_response('station', {
        'station_id': 103, 'time_after': '01:00'
    })
    assert code == 400
    assert response['errors'] == {'time_after': ['parameter time_after without parameter time_before']}

    code, response = _get_response('station', {
        'station_id': 103, 'time_before': '01:00'
    })
    assert code == 400
    assert response['errors'] == {'time_before': ['parameter time_before without parameter time_after']}


def test_station_city_stations_view():
    settlement = create_settlement(country=Country.RUSSIA_ID, title_ru='Город', title_ru_genitive='Города')

    station1 = create_station(
        settlement=settlement, id=101, t_type=TransportType.TRAIN_ID, type_choices='train', title='вокзал1'
    )
    station2 = create_station(
        settlement=settlement, id=102, t_type=TransportType.TRAIN_ID, type_choices='suburban', title='вокзал2'
    )
    station3 = create_station(
        settlement=settlement, id=103, t_type=TransportType.PLANE_ID, type_choices='tablo', title='аэропорт'
    )
    station4 = create_station(
        settlement=settlement, id=104, t_type=TransportType.BUS_ID, type_choices='schedule', title='остановка'
    )
    station5 = create_station(
        settlement=settlement, id=105, t_type=TransportType.WATER_ID, type_choices='schedule', title='порт'
    )

    station12 = create_station(id=112, t_type=TransportType.TRAIN_ID, type_choices='train,suburban')
    station13 = create_station(id=113, t_type=TransportType.PLANE_ID, type_choices='tablo')
    station14 = create_station(id=114, t_type=TransportType.BUS_ID, type_choices='schedule')
    station15 = create_station(id=115, t_type=TransportType.WATER_ID, type_choices='schedule')

    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[[None, 0, station1], [10, None, station12]]
    )
    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station2], [10, None, station12]]
    )
    create_thread(
        t_type=TransportType.PLANE_ID,
        schedule_v1=[[None, 0, station3], [10, None, station13]]
    )
    create_thread(
        t_type=TransportType.BUS_ID,
        schedule_v1=[[None, 0, station4], [10, None, station14]]
    )
    create_thread(
        t_type=TransportType.WATER_ID,
        schedule_v1=[[None, 0, station5], [10, None, station15]]
    )

    code, response = _get_response('station/city_stations', {'station_id': 101})

    assert code == 200
    assert_that(response['result'], has_entries({
        'settlement': has_entries({
            'title': 'Город',
            'titleGenitive': 'Города'
        }),
        'cityStations': has_entries({
            'train': contains(
                has_entries({
                    'id': 101, 'title': 'вокзал1',
                    'mainSubtype': 'train', 'subtypes': ['train']
                }),
                has_entries({
                    'id': 102, 'title': 'вокзал2',
                    'mainSubtype': 'suburban', 'subtypes': ['suburban']
                })
            ),
            'plane': contains(
                has_entries({
                    'id': 103, 'title': 'аэропорт',
                    'mainSubtype': 'plane', 'subtypes': ['plane']
                })
            ),
            'bus': contains(
                has_entries({
                    'id': 104, 'title': 'остановка',
                    'mainSubtype': 'schedule', 'subtypes': ['schedule']
                })
            ),
            'water': contains(
                has_entries({
                    'id': 105, 'title': 'порт',
                    'mainSubtype': 'schedule', 'subtypes': ['schedule']
                })
            )
        })
    }))


def test_station_city_stations_bad_response():
    station1 = create_station(id=101, t_type=TransportType.TRAIN_ID, type_choices='train,suburban')
    station2 = create_station(id=102)
    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station1], [10, None, station2]]
    )

    code, response = _get_response('station/city_stations', {})
    assert code == 400
    assert response['errors'] == {'station_id': ['station_id should be in the request params']}

    code, response = _get_response('station/city_stations', {
        'station_id': 100
    })
    assert code == 404
    assert response['errors'] == ['Станции с id 100 нет в базе']


def test_station_popular_directions_view():
    station0 = create_station(id=101, title='Station0', slug='station0', type_choices='suburban')
    settlement1 = create_settlement(id=110, title='City1', slug='city1')
    station1 = create_station(
        id=111, title='Station1', slug='station1', settlement=settlement1, type_choices='suburban'
    )
    station2 = create_station(id=121, title='Station2', slug='station2', type_choices='suburban')
    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station0], [10, 15, station1], [20, 25, station2], [30, None, station0]]
    )

    with mock.patch.object(
            search_stats, 'get_top_from',
            return_value=[('c110', 100), ('s111', 90), ('s121', 80)]
    ):
        with mock.patch.object(
                search_stats, 'get_top_to',
                return_value=[('s111', 100), ('c110', 90), ('s121', 80)]
        ):
            code, response = _get_response('station/popular_directions', {'station_id': 101, 'limit': 2})

            assert code == 200
            assert_that(response['result'], has_entries({
                'searchTransportType': None,
                'station': has_entries({
                    'key': 's101',
                    'slug': 'station0',
                    'title': 'Station0',
                }),
                'from': contains(
                    {'key': 's111', 'slug': 'station1', 'title': 'City1'},
                    {'key': 's121', 'slug': 'station2', 'title': 'Station2'},
                ),
                'to': contains(
                    {'key': 's111', 'slug': 'station1', 'title': 'Station1'},
                    {'key': 's121', 'slug': 'station2', 'title': 'Station2'},
                )
            }))


def test_station_popular_directions_bad_response():
    station1 = create_station(id=101, t_type=TransportType.TRAIN_ID, type_choices='train,suburban')
    station2 = create_station(id=102)
    create_thread(
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station1], [10, None, station2]]
    )

    code, response = _get_response('station/popular_directions', {})
    assert code == 400
    assert response['errors'] == {'station_id': ['station_id should be in the request params']}

    code, response = _get_response('station/popular_directions', {
        'station_id': 100
    })
    assert code == 404
    assert response['errors'] == ['Станции с id 100 нет в базе']
