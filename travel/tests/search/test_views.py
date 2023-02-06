# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import pytest
from django.test import Client
from hamcrest import assert_that, has_entries

from common.tester.factories import create_settlement, create_station
from common.tester.utils.replace_setting import replace_setting
from common.utils.date import MSK_TZ
from travel.rasp.train_api.wizard_api.factories import (
    WizardDirectionSegmentFactory, WizardDirectionFactory, WizardDirectionSegmentTrainFactory
)

pytestmark = [pytest.mark.dbuser]


@pytest.fixture(autouse=True)
def replace_train_wizard_api_host():
    with replace_setting('TRAIN_WIZARD_API_DIRECTION_HOST', 'train-wizard-api.net'):
        yield


def test_train_segment(httpretty):
    departure = MSK_TZ.localize(datetime(2019, 2, 16, 0, 41))
    arrival = MSK_TZ.localize(datetime(2019, 2, 16, 9, 13))
    departure_station = create_station(
        settlement=213,
        title='Ленинградский вокзал',
        country=225,
        __={
            'codes': {'express': '222'}
        }
    )
    arrival_station = create_station(
        settlement=create_settlement(id=2),
        title='Ладожский вокзал',
        country=225,
        __={
            'codes': {'express': '333'}
        }
    )
    wizard_response = WizardDirectionFactory(segments=[WizardDirectionSegmentFactory(
        departure_station=departure_station,
        departure_dt=departure,
        arrival_station=arrival_station,
        arrival_dt=arrival,
    )])
    httpretty.register_uri(
        httpretty.GET,
        'https://train-wizard-api.net/searcher/public-api/open_direction/',
        body=json.dumps(wizard_response)
    )

    params = {
        'pointFrom': 'c213',
        'pointTo': 'c2',
        'departure': datetime(2019, 2, 16, 0, 41).strftime('%Y-%m-%dT%H:%M'),
        'number': '016А'
    }
    response = Client().get('/ru/api/search/train-segment/', params)

    assert response.status_code == 200
    assert_that(response.data['result'], has_entries(
        arrival='2019-02-16T06:13:00Z',
        departure='2019-02-15T21:41:00Z',
        company=has_entries(
            title='ФПК',
            shortTitle='ФПК',
        ),
        duration=512 * 60,
        number='016А',
        stationFrom=has_entries(
            title='Ленинградский вокзал',
            shortTitle='Ленинградский вокзал',
            id=departure_station.id,
            timezone=departure_station.pytz.zone,
            country=has_entries(
                id=225,
                code='RU'
            ),
            railwayTimezone='Europe/Moscow',
            codes=has_entries(
                express='222'
            )
        ),
        stationTo=has_entries(
            title='Ладожский вокзал',
            shortTitle='Ладожский вокзал',
            id=arrival_station.id,
            timezone=arrival_station.pytz.zone,
            country=has_entries(
                id=225,
                code='RU'
            ),
            railwayTimezone='Europe/Moscow',
            codes=has_entries(
                express='333'
            )
        ),
        title="Москва — Мурманск",
        thread=has_entries(
            title="Москва — Мурманск",
            number='016А',
            deluxeTrain=has_entries(
                title="Арктика",
                shortTitle="фирменный «Арктика»",
                isDeluxe=True,
                isHighSpeed=False,
            ),
            firstCountryCode='RU',
            lastCountryCode='UA',
        ),
        transport={'code': 'train', 'id': 1, 'title': 'Поезд'},
    ))
    assert len(httpretty.latest_requests) == 1


def test_empty_brand(httpretty):
    wizard_response = WizardDirectionFactory(segments=[WizardDirectionSegmentFactory(
        train__brand=None,
        departure_station=create_station(settlement=213, title='Ленинградский вокзал'),
        arrival_station=create_station(settlement=create_settlement(id=2), title='Ладожский вокзал'),
    )])
    httpretty.register_uri(
        httpretty.GET,
        'https://train-wizard-api.net/searcher/public-api/open_direction/',
        body=json.dumps(wizard_response)
    )

    params = {
        'pointFrom': 'c213',
        'pointTo': 'c2',
        'departure': datetime(2019, 2, 16, 0, 41).strftime('%Y-%m-%dT%H:%M'),
        'number': '016А'
    }
    response = Client().get('/ru/api/search/train-segment/', params)

    assert response.status_code == 200
    assert_that(response.data['result'], has_entries(
        thread=has_entries(
            deluxeTrain=None,
        ),
    ))
    assert len(httpretty.latest_requests) == 1


def test_allmost_empty_thread(httpretty):
    wizard_response = WizardDirectionFactory(segments=[WizardDirectionSegmentFactory(
        departure_station=create_station(settlement=213, title='Ленинградский вокзал'),
        arrival_station=create_station(settlement=create_settlement(id=2), title='Ладожский вокзал'),
        train__is_suburban=None,
        train__has_dynamic_pricing=None,
        train__title='Усинск — Сыктывкар',
        train__display_number=None,
        train__brand=None,
        train__two_storey=None,
        train__number='111Й',
        train__coach_owners=None,
    )])
    httpretty.register_uri(
        httpretty.GET,
        'https://train-wizard-api.net/searcher/public-api/open_direction/',
        body=json.dumps(wizard_response)
    )

    params = {
        'pointFrom': 'c213',
        'pointTo': 'c2',
        'departure': datetime(2019, 2, 16, 0, 41).strftime('%Y-%m-%dT%H:%M'),
        'number': '111Й'
    }
    response = Client().get('/ru/api/search/train-segment/', params)

    assert response.status_code == 200
    assert_that(response.data['result'], has_entries(
        company=has_entries(
            title='',
            shortTitle='',
        ),
        number='111Й',
        title='Усинск — Сыктывкар',
        thread=has_entries(
            title='Усинск — Сыктывкар',
            number='111Й',
            deluxeTrain=None,
        ),
    ))
    assert len(httpretty.latest_requests) == 1


@pytest.mark.parametrize('segments_kwargs, expected_result', [
    (
        [
            {'train': WizardDirectionSegmentTrainFactory(number='016Й'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 11))},
            {'train': WizardDirectionSegmentTrainFactory(number='016А'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 9))},
            {'train': WizardDirectionSegmentTrainFactory(number='016Б'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 7))},
        ], {'arrival': '2019-02-16T06:00:00Z'}
    ),
    (
        [
            {'train': WizardDirectionSegmentTrainFactory(display_number=None),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 9))},
            {'train': WizardDirectionSegmentTrainFactory(display_number='!o16a!'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 7))},
        ], {'arrival': '2019-02-16T04:00:00Z'}
    ),
    (
        [
            {'train': WizardDirectionSegmentTrainFactory(thread_type='any'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 9))},
            {'train': WizardDirectionSegmentTrainFactory(thread_type='through_train'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 7))},
        ], {'arrival': '2019-02-16T06:00:00Z'}
    ),
    (
        [
            {'duration': 300,
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 9))},
            {'duration': 200,
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 7))},
        ], {'arrival': '2019-02-16T04:00:00Z'}
    ),
    (
        [
            {'train': WizardDirectionSegmentTrainFactory(thread_type='through_train'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 11)),
             'duration': 400},
            {'train': WizardDirectionSegmentTrainFactory(thread_type='any'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 9)),
             'duration': 300},
            {'train': WizardDirectionSegmentTrainFactory(thread_type='through_train'),
             'arrival_dt': MSK_TZ.localize(datetime(2019, 2, 16, 7)),
             'duration': 200},
        ], {'arrival': '2019-02-16T06:00:00Z'}
    ),
])
def test_chose_train_segment(httpretty, segments_kwargs, expected_result):
    departure = MSK_TZ.localize(datetime(2019, 2, 16, 0, 41))
    arrival = MSK_TZ.localize(datetime(2019, 2, 16, 9, 13))
    departure_station = create_station(settlement=213, title='Ленинградский вокзал')
    arrival_station = create_station(settlement=create_settlement(id=2), title='Ладожский вокзал')
    for kw in segments_kwargs:
        kw.setdefault('departure_station', departure_station)
        kw.setdefault('departure_dt', departure)
        kw.setdefault('arrival_station', arrival_station)
        kw.setdefault('arrival_dt', arrival)
    wizard_response = WizardDirectionFactory(segments=[
        WizardDirectionSegmentFactory(**kwargs) for kwargs in segments_kwargs
    ])
    httpretty.register_uri(
        httpretty.GET,
        'https://train-wizard-api.net/searcher/public-api/open_direction/',
        body=json.dumps(wizard_response)
    )

    params = {
        'pointFrom': 'c213',
        'pointTo': 'c2',
        'departure': datetime(2019, 2, 16, 0, 41).strftime('%Y-%m-%dT%H:%M'),
        'number': '016А'
    }
    response = Client().get('/ru/api/search/train-segment/', params)

    assert response.status_code == 200
    assert_that(response.data['result'], has_entries(**expected_result))
    assert len(httpretty.latest_requests) == 1
