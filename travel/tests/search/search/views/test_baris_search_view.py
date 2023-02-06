# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import pytest
from django.test import Client
from hamcrest import has_entries, assert_that, contains_inanyorder, contains

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.transport import TransportType
from common.tester.factories import create_station, create_company, create_settlement, create_transport_model
from common.tester.utils.datetime import replace_now


pytestmark = [pytest.mark.dbuser]

create_plane_station = create_station.mutate(t_type=TransportType.PLANE_ID, type_choices='tablo')


@replace_now('2020-06-01')
@pytest.mark.parametrize('yandex_avia_code, expected_url', [
    ('su_aeroflot', 'https://travel-test.yandex.ru/avia/airline/su_aeroflot/?utm_medium=rasp_airline&utm_source=rasp'),
    ('', None),
    (None, None)
])
def test_company_yandex_avia_url(yandex_avia_code, expected_url):
    station_from = create_plane_station(id=101)
    station_to = create_plane_station(id=102)
    create_company(yandex_avia_code=yandex_avia_code, id=301)

    with mock_baris_response({
        'flights': [{
            'airlineID': 301,
            'departureStation': 101,
            'arrivalStation': 102,
            'title': 'SU 1',
            'route': [101, 102],
            'transportModelID': 201,
            'departureDatetime': '2020-06-01T01:30:00+05:00',
            'arrivalDatetime': '2020-06-01T05:00:00+05:00',
        }]
    }):
        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'when': '2020-06-01',
            'transportType': 'plane'
        }).content)

    assert_that(response['result']['segments'], contains_inanyorder(
        has_entries({
            'company': has_entries({
                'yandexAviaUrl': expected_url,
            })
        }),
    ))


def _create_db_items():
    settlement1 = create_settlement(id=91, title='От', slug='ot', time_zone='Etc/GMT-3')
    settlement2 = create_settlement(id=92, title='До', slug='do', time_zone='Etc/GMT-5')
    create_plane_station(id=100, title='Раньше')
    create_plane_station(id=101, settlement=settlement1, time_zone='Etc/GMT-3', title='от')
    create_plane_station(id=102, settlement=settlement2, time_zone='Etc/GMT-5', title='до')
    create_company(id=301, title='Компания1', url='url1', yandex_avia_code='Company1')
    create_company(id=302, title='Компания2', url='url2', yandex_avia_code='Company2')
    create_transport_model(id=201, title='Самолет1')


ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [101],
    'arrivalStations': [102],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureDatetime': '2020-06-01T01:30:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 101,
            'arrivalDatetime': '2020-06-01T05:00:00+05:00',
            'arrivalTerminal': '',
            'arrivalStation': 102,
            'transportModelID': 201,
            'codeshares': [{
                'airlineID': 302,
                'title': 'SV 1'
            }],
            'route': [100, 101, 102],
            'source': 'flight-board',
        }
    ]
}


@replace_now('2020-06-01')
@pytest.mark.parametrize('nearest, latest_datetime, when', [
    (False, '2020-06-02T01:00:00+00:00', '2020-06-01'),
    (False, '2020-06-02T01:00:00+00:00', 'today'),
    (True, '2020-05-31T22:30:00+00:00', None)
])
def test_baris_one_day_search(nearest, latest_datetime, when):
    _create_db_items()

    with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
        search_params = {
            'national_version': 'ru',
            'transportType': 'plane',
            'pointFrom': 'c91',
            'pointTo': 'c92',
            'nearest': nearest,
        }
        if not nearest:
            search_params['when'] = when

        response = Client().get('/ru/search/search/', search_params)

        if nearest:
            assert response.data['result']['canonical'] is None
        else:
            assert_that(response.data['result']['canonical'], has_entries({
                'transportType': 'plane',
                'pointFrom': 'ot',
                'pointTo': 'do',
            }))

        assert_that(response.data['result'], has_entries({
            'archivalData': None,
            'context': has_entries({
                'transportTypes': ['plane'],
                'isChanged': False,
                'latestDatetime': latest_datetime,
                'search': has_entries({
                    'nearest': nearest,
                    'pointFrom': has_entries({
                        'titleWithType': 'г. От',
                        'title': 'От',
                        'key': 'c91',
                        'slug': 'ot',
                    }),
                    'pointTo': has_entries({
                        'titleWithType': 'г. До',
                        'title': 'До',
                        'key': 'c92',
                        'slug': 'do',
                    })
                }),
                'original': has_entries({
                    'nearest': nearest,
                    'pointFrom': has_entries({
                        'titleWithType': 'г. От',
                        'title': 'От',
                        'key': 'c91',
                        'slug': 'ot',
                    }),
                    'pointTo': has_entries({
                        'titleWithType': 'г. До',
                        'title': 'До',
                        'key': 'c92',
                        'slug': 'do',
                    })
                })
            }),

            'segments': contains(
                has_entries({
                    'departure': '2020-05-31T22:30:00+00:00',
                    'arrival': '2020-06-01T00:00:00+00:00',
                    'departureLocalDt': '2020-06-01T01:30:00+03:00',
                    'arrivalLocalDt': '2020-06-01T05:00:00+05:00',
                    'startDate': '2020-06-01',
                    'departureEvent': None,
                    'thread': has_entries({
                        'number': 'SU 1',
                        'title': 'Раньше \u2013 До',
                        'comment': '',
                        'uid': '',
                        'density': '',
                        'beginTime': None,
                        'endTime': None,
                        'schedulePlanCode': None,
                        'isAeroExpress': False,
                        'isExpress': False,
                        'isBasic': True
                    }),
                    'isThroughTrain': False,
                    'title': 'Раньше \u2013 До',
                    'company': has_entries({
                        'title': 'Компания1',
                        'url': 'url1',
                        'yandexAviaUrl': 'https://travel-test.yandex.ru/avia/airline/Company1/?utm_medium=rasp_airline&utm_source=rasp',
                        'hidden': False,
                        'id': 301
                    }),
                    'suburbanFacilities': None,
                    'number': 'SU 1',
                    'stops': '',
                    'departureEventKey': None,
                    'isInterval': False,
                    'tariffsKeys': ['SU 1'] if nearest else ['daemon SU-1 0601'],
                    'stationFrom': has_entries({
                        'settlementId': 91,
                        'title': 'от',
                        'id': 101
                    }),
                    'stationTo': has_entries({
                        'settlementId': 92,
                        'title': 'до',
                        'id': 102
                    }),
                    'duration': 5400,
                    'arrivalEvent': None,
                    'arrivalEventKey': None,
                    'transport': has_entries({
                        'model': {'title': 'Самолет1'},
                        'code': 'plane',
                        'id': 2,
                        'title': 'Самолёт'
                    })
                })
            )
        }))

        if not nearest:
            assert_that(response.data['result']['segments'][0]['codeshares'][0], has_entries({
                'number': 'SV 1',
                'tariffsKeys': ['daemon SV-1 0601'],
                'company': has_entries({
                    'title': 'Компания2',
                    'url': 'url2',
                    'yandexAviaUrl': 'https://travel-test.yandex.ru/avia/airline/Company2/?utm_medium=rasp_airline&utm_source=rasp',
                    'hidden': False,
                    'id': 302
                }),
            }))


ALL_DAYS_P2P_BARIS_RESPONSE = {
    'departureStations': [101],
    'arrivalStations': [102],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureTime': '01:30',
            'departureTimezone': '+0300',
            'departureStation': 101,
            'arrivalTime': '05:00',
            'arrivalTimezone': '+0500',
            'arrivalStation': 102,
            'arrivalDayShift': 0,
            'transportModelID': 201,
            'codeshares': [{
                'airlineID': 302,
                'title': 'SV 1'
            }],
            'route': [100, 101, 102],
            'source': 'flight-board',
            'masks': [
                {
                    'from': '2020-06-01',
                    'until': '2020-06-30',
                    'on': 2
                }
            ]
        }
    ]
}


@replace_now('2020-06-01')
def test_baris_all_days_search():
    _create_db_items()

    with mock_baris_response(ALL_DAYS_P2P_BARIS_RESPONSE):
        search_params = {
            'national_version': 'ru',
            'transportType': 'plane',
            'pointFrom': 'c91',
            'pointTo': 'c92',
        }

        response = Client().get('/ru/search/search/', search_params)

        assert_that(response.data['result'], has_entries({
            'canonical': has_entries({
                'transportType': 'plane',
                'pointFrom': 'ot',
                'pointTo': 'do',
            }),
            'archivalData': None,
            'context': has_entries({
                'transportTypes': ['plane'],
                'isChanged': False,
                'latestDatetime': None,
                'search': has_entries({
                    'nearest': False,
                    'pointFrom': has_entries({
                        'titleWithType': 'г. От',
                        'title': 'От',
                        'key': 'c91',
                        'slug': 'ot',
                    }),
                    'pointTo': has_entries({
                        'titleWithType': 'г. До',
                        'title': 'До',
                        'key': 'c92',
                        'slug': 'do',
                    })
                }),
                'original': has_entries({
                    'nearest': False,
                    'pointFrom': has_entries({
                        'titleWithType': 'г. От',
                        'title': 'От',
                        'key': 'c91',
                        'slug': 'ot',
                    }),
                    'pointTo': has_entries({
                        'titleWithType': 'г. До',
                        'title': 'До',
                        'key': 'c92',
                        'slug': 'do',
                    })
                })
            }),

            'segments': contains(
                has_entries({
                    'departure': '2020-06-01T22:30:00+00:00',
                    'arrival': '2020-06-02T00:00:00+00:00',
                    'departureLocalDt': '2020-06-02T01:30:00+03:00',
                    'arrivalLocalDt': '2020-06-02T05:00:00+05:00',
                    'startDate': '2020-06-02',
                    'daysByTimezone': {
                        'Etc/GMT-3': {'text': '2, 9, 16, 23, 30 июня'}
                    },
                    'runDays': {
                        '2020': {
                            '6': [
                                0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1
                            ]
                        }
                    },
                    'departureEvent': None,
                    'thread': has_entries({
                        'number': 'SU 1',
                        'title': 'Раньше \u2013 До',
                        'comment': '',
                        'uid': '',
                        'density': '',
                        'beginTime': None,
                        'endTime': None,
                        'schedulePlanCode': None,
                        'isAeroExpress': False,
                        'isExpress': False,
                        'isBasic': True
                    }),
                    'isThroughTrain': False,
                    'title': 'Раньше \u2013 До',
                    'company': has_entries({
                        'title': 'Компания1',
                        'url': 'url1',
                        'yandexAviaUrl': 'https://travel-test.yandex.ru/avia/airline/Company1/?utm_medium=rasp_airline&utm_source=rasp',
                        'hidden': False,
                        'id': 301
                    }),
                    'suburbanFacilities': None,
                    'number': 'SU 1',
                    'stops': '',
                    'departureEventKey': None,
                    'isInterval': False,
                    'tariffsKeys': ['SU 1'],
                    'stationFrom': has_entries({
                        'settlementId': 91,
                        'title': 'от',
                        'id': 101
                    }),
                    'stationTo': has_entries({
                        'settlementId': 92,
                        'title': 'до',
                        'id': 102
                    }),
                    'duration': 5400,
                    'arrivalEvent': None,
                    'arrivalEventKey': None,
                    'transport': has_entries({
                        'model': {'title': 'Самолет1'},
                        'code': 'plane',
                        'id': 2,
                        'title': 'Самолёт'
                    })
                })
            )
        }))
