# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import pytest
from django.test import Client
from hamcrest import has_entries, assert_that, contains, contains_inanyorder

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.transport import TransportType
from common.tester.factories import (
    create_station, create_company, create_settlement, create_transport_model, create_thread, create_country
)
from common.tester.utils.datetime import replace_now


pytestmark = [pytest.mark.dbuser]


def _create_db_items():
    country = create_country()
    settlement1 = create_settlement(id=91, title='От', slug='ot', time_zone='Etc/GMT-3', country=country)
    settlement2 = create_settlement(id=92, title='До', slug='do', time_zone='Etc/GMT-5', country=country)
    create_station(
        id=101, settlement=settlement1, time_zone='Etc/GMT-3', title='Аэропорт от',
        t_type=TransportType.PLANE_ID, country=country, type_choices='tablo'
    )
    create_station(
        id=102, settlement=settlement2, time_zone='Etc/GMT-5', title='Аэропорт до',
        t_type=TransportType.PLANE_ID, country=country, type_choices='tablo'
    )
    station3 = create_station(
        id=103, settlement=settlement1, time_zone='Etc/GMT-3', title='Вокзал от',
        t_type=TransportType.TRAIN_ID, country=country, type_choices='train'
    )
    station4 = create_station(
        id=104, settlement=settlement2, time_zone='Etc/GMT-5', title='Вокзал до',
        t_type=TransportType.TRAIN_ID, country=country, type_choices='train'
    )
    create_company(id=301, title='Компания1', url='url1', yandex_avia_code='Company1')
    create_transport_model(id=201, title='Самолет1')

    create_thread(
        t_type=TransportType.TRAIN_ID,
        number='Й1', title='Поезд', uid='uid',
        schedule_v1=[[None, 0, station3], [300, None, station4]],
        year_days=[date(2020, 6, 1)], tz_start_time='08:00',
        __={'calculate_noderoute': True}
    )


ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [101],
    'arrivalStations': [102],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureDatetime': '2020-06-01T01:30:00+03:00',
            'departureStation': 101,
            'arrivalDatetime': '2020-06-01T05:00:00+05:00',
            'arrivalTerminal': '',
            'arrivalStation': 102,
            'transportModelID': 201,
            'route': [101, 102],
            'source': 'flight-board',
        }
    ]
}


@replace_now('2020-06-01')
@pytest.mark.parametrize('nearest, latest_datetime, when', [
    (False, '2020-06-02T01:00:00+00:00', '2020-06-01'),
    (False, '2020-06-02T01:00:00+00:00', 'today'),
    (True, '2020-06-01T05:00:00+00:00', None)
])
def test_all_types_one_day_search(nearest, latest_datetime, when):
    _create_db_items()

    with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
        search_params = {
            'national_version': 'ru',
            'pointFrom': 'c91',
            'pointTo': 'c92',
            'nearest': nearest,
        }
        if not nearest:
            search_params['when'] = when

        response = Client().get('/ru/search/search/', search_params)

        if not nearest:
            assert_that(response.data['result']['canonical'], has_entries({
                'transportType': None,
                'pointFrom': 'ot',
                'pointTo': 'do',
            }))
        else:
            assert response.data['result']['canonical'] is None

        assert_that(response.data['result'], has_entries({
            'archivalData': None,
            'context': has_entries({
                'transportTypes': contains_inanyorder('plane', 'train'),
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
                        'title': 'От \u2013 До',
                    }),
                    'isThroughTrain': False,
                    'title': 'От \u2013 До',
                    'company': has_entries({
                        'title': 'Компания1',
                        'url': 'url1',
                        'id': 301
                    }),
                    'number': 'SU 1',
                    'stationFrom': has_entries({
                        'settlementId': 91,
                        'title': 'Аэропорт от',
                        'id': 101
                    }),
                    'stationTo': has_entries({
                        'settlementId': 92,
                        'title': 'Аэропорт до',
                        'id': 102
                    }),
                    'duration': 5400,
                    'transport': has_entries({
                        'model': {'title': 'Самолет1'},
                        'code': 'plane',
                        'id': 2,
                        'title': 'Самолёт'
                    })
                }),

                has_entries({
                    'departure': '2020-06-01T05:00:00+00:00',
                    'arrival': '2020-06-01T10:00:00+00:00',
                    'departureLocalDt': '2020-06-01T08:00:00+03:00',
                    'arrivalLocalDt': '2020-06-01T15:00:00+05:00',
                    'startDate': '2020-06-01',
                    'thread': has_entries({
                        'number': 'Й1',
                        'title': 'Поезд',
                        'comment': '',
                        'uid': 'uid'
                    }),
                    'isThroughTrain': False,
                    'title': 'Поезд',
                    'number': 'Й1',
                    'stationFrom': has_entries({
                        'settlementId': 91,
                        'title': 'Вокзал от',
                        'id': 103
                    }),
                    'stationTo': has_entries({
                        'settlementId': 92,
                        'title': 'Вокзал до',
                        'id': 104
                    }),
                    'duration': 18000,
                    'transport': has_entries({
                        'code': 'train',
                        'id': 1,
                        'title': 'Поезд'
                    })
                })
            )
        }))

        segments = response.data['result']['segments']
        if nearest:
            assert segments[0]['tariffsKeys'] == ['SU 1']
            assert_that(segments[1]['tariffsKeys'], contains_inanyorder(
                'static 103 104 uid',
                'Й1'
            ))
        else:
            assert segments[0]['tariffsKeys'] == ['daemon SU-1 0601']
            assert_that(segments[1]['tariffsKeys'], contains_inanyorder(
                u'daemon Й1 0601',
                u'static 103 104 uid 0601',
                u'train Й1 20200601_0800'
            ))


ALL_DAYS_P2P_BARIS_RESPONSE = {
    'departureStations': [101],
    'arrivalStations': [102],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureTime': '01:30',
            'departureStation': 101,
            'arrivalTime': '05:00',
            'arrivalStation': 102,
            'arrivalDayShift': 0,
            'transportModelID': 201,
            'route': [101, 102],
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
def test_all_types_all_days_search():
    _create_db_items()

    with mock_baris_response(ALL_DAYS_P2P_BARIS_RESPONSE):
        search_params = {
            'national_version': 'ru',
            'pointFrom': 'c91',
            'pointTo': 'c92',
            'timezones': 'Etc/GMT-3'
        }

        response = Client().get('/ru/search/search/', search_params)

        assert_that(response.data['result'], has_entries({
            'canonical': has_entries({
                'transportType': None,
                'pointFrom': 'ot',
                'pointTo': 'do',
            }),
            'archivalData': None,
            'context': has_entries({
                'transportTypes': contains_inanyorder('plane', 'train'),
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
                    'thread': has_entries({
                        'number': 'SU 1',
                        'title': 'От \u2013 До'
                    }),
                    'isThroughTrain': False,
                    'title': 'От \u2013 До',
                    'company': has_entries({
                        'title': 'Компания1',
                        'url': 'url1',
                        'id': 301
                    }),
                    'number': 'SU 1',
                    'tariffsKeys': ['SU 1'],
                    'stationFrom': has_entries({
                        'settlementId': 91,
                        'title': 'Аэропорт от',
                        'id': 101
                    }),
                    'stationTo': has_entries({
                        'settlementId': 92,
                        'title': 'Аэропорт до',
                        'id': 102
                    }),
                    'duration': 5400,
                    'transport': has_entries({
                        'model': {'title': 'Самолет1'},
                        'code': 'plane',
                        'id': 2,
                        'title': 'Самолёт'
                    })
                }),

                has_entries({
                    'departure': '2020-06-01T05:00:00+00:00',
                    'arrival': '2020-06-01T10:00:00+00:00',
                    'departureLocalDt': '2020-06-01T08:00:00+03:00',
                    'arrivalLocalDt': '2020-06-01T15:00:00+05:00',
                    'startDate': '2020-06-01',
                    'daysByTimezone': {
                        'Etc/GMT-3': {'text': 'только 1 июня'}
                    },
                    'runDays': {
                        '2020': {
                            '6': [
                                1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                            ]
                        }
                    },
                    'thread': has_entries({
                        'number': 'Й1',
                        'title': 'Поезд',
                        'comment': '',
                        'uid': 'uid'
                    }),
                    'isThroughTrain': False,
                    'title': 'Поезд',
                    'number': 'Й1',
                    'tariffsKeys': contains_inanyorder(
                        'static 103 104 uid',
                        'Й1'
                    ),
                    'stationFrom': has_entries({
                        'settlementId': 91,
                        'title': 'Вокзал от',
                        'id': 103
                    }),
                    'stationTo': has_entries({
                        'settlementId': 92,
                        'title': 'Вокзал до',
                        'id': 104
                    }),
                    'duration': 18000,
                    'transport': has_entries({
                        'code': 'train',
                        'id': 1,
                        'title': 'Поезд'
                    })
                })
            )
        }))
