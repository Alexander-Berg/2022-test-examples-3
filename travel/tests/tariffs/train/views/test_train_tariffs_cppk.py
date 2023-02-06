# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
from django.test import Client
from django.utils.http import urlencode
from hamcrest import assert_that, has_entries, contains_string

from common.apps.train_order.enums import CoachType
from common.models.geo import Country, Settlement, StationMajority
from common.models.transport import TransportType, TrainPseudoStationMap
from common.tester.factories import (
    create_settlement, create_station, create_currency
)
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_dynamic_setting_key
from common.utils.date import MSK_TIMEZONE
from common.utils.title_generator import DASH
from travel.rasp.train_api.tariffs.train.factories.base import create_www_setting_cache_timeouts
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.send_query import TRAIN_PRICING_ENDPOINT
from travel.rasp.train_api.train_bandit_api.logging import bandit_train_details_logger
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


@pytest.fixture
def im_train_pricing_mock(httpretty):
    mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=ImTrainPricingResponseFactory(**{
        'DestinationCode': '2000000',
        'DestinationStationCode': '2006004',
        'OriginCode': '2004000',
        'OriginStationCode': '2004001',
        'Trains': [{
            'Carriers': ['ЦППК'],
            'DepartureDateTime': '2016-05-15T17:26:00',
            'DestinationName': 'С-ПЕТЕР-ГЛ',
            'DestinationStationCode': 'real_to',
            'OriginName': 'МОСКВА ОКТ',
            'OriginStationCode': 'real_from',
            'TrainNumber': '020У',
            'TrainNumberToGetRoute': '020У',
            'DisplayTrainNumber': '020У',
            'CarGroups': [
                {
                    'CarType': CoachType.PLATZKARTE,
                    'MaxPrice': 1929.2,
                    'MinPrice': 1212.8,
                    'TotalPlaceQuantity': 138,
                },
                {
                    'CarType': CoachType.COMPARTMENT,
                    'MaxPrice': 3708.0,
                    'MinPrice': 3075.9,
                    'TotalPlaceQuantity': 34,
                }
            ],
            'Provider': 'P2',
        }]
    }))
    return httpretty


@pytest.fixture
def m_bandit_train_details_log():
    with replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_LOGGING', True):
        with mock.patch.object(bandit_train_details_logger, 'info') as m_:
            yield m_


@replace_now('2016-04-20')
@pytest.mark.usefixtures('worker_cache_stub')
@replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', True)
@replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', False)  # noqa: E126
def test_init_and_poll_query_cppk_movista(trainslist_mock, worker_stub):
    create_www_setting_cache_timeouts()
    create_currency(code='RUR', iso_code='RUB')

    t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)

    piter = create_settlement(title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)
    pseudo_station_from = create_station(__={'codes': {'express': 'pseudo_from'}}, settlement=piter,
                                         t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID)
    station_from = create_station(__={'codes': {'express': 'real_from'}}, settlement=piter,
                                  t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)
    station_to = create_station(__={'codes': {'express': 'real_to'}}, settlement=Settlement.MOSCOW_ID,
                                t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)

    TrainPseudoStationMap.objects.create(number='020У', pseudo_station=pseudo_station_from, station=station_from)

    response = Client().get('/ru/api/segments/train-tariffs/?{}'.format(
        urlencode({
            'pointFrom': 'c{}'.format(piter.id),
            'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
            'date': '2016-05-15',
            'national_version': 'ru'
        }, doseq=True)
    ))
    assert worker_stub.call_count == 1
    assert response.status_code == 200
    assert response.data == {
        'segments': [],
        'querying': True
    }

    response = Client().get('/ru/api/segments/train-tariffs/?{}'.format(
        urlencode({
            'pointFrom': 'c{}'.format(piter.id),
            'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
            'date': '2016-05-15',
            'national_version': 'ru'
        }, doseq=True)
    ))
    assert worker_stub.call_count == 1
    assert response.status_code == 200
    query_again_result = json.loads(response.content)

    response = Client().get('/ru/api/segments/train-tariffs/poll/?{}'.format(
        urlencode({
            'pointFrom': 'c{}'.format(piter.id),
            'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
            'date': '2016-05-15',
            'national_version': 'ru'
        }, doseq=True)
    ))
    assert worker_stub.call_count == 1
    assert response.status_code == 200
    poll_result = response.data

    assert poll_result == query_again_result
    assert not poll_result['querying']

    assert len(poll_result['segments']) == 1

    segment = poll_result['segments'][0]

    assert_that(segment['tariffs'], has_entries({
        'electronicTicket': True,
        'classes': has_entries({
            'compartment': has_entries({
                'price': {'currency': 'RUB', 'value': 3075.9},
                'seats': 34, 'severalPrices': True,
                'orderUrl': contains_string('cls=compartment'),
                'trainOrderUrlOwner': 'trains',
                'trainOrderUrl': contains_string('provider=P1'),
            }),
            'platzkarte': has_entries({
                'price': {'currency': 'RUB', 'value': 1212.8},
                'seats': 138, 'severalPrices': True,
                'orderUrl': contains_string('cls=platzkarte'),
                'trainOrderUrlOwner': 'trains',
                'trainOrderUrl': contains_string('provider=P1'),
            })
        }),
    }))

    assert_that(segment, has_entries({
        'number': '020У',
        'title': 'МОСКВА ОКТ {} С-ПЕТЕР-ГЛ'.format(DASH),
        'ufsTitle': 'МОСКВА ОКТ {} С-ПЕТЕР-ГЛ'.format(DASH),
        'canSupplySegments': True,
        'stationFrom': has_entries({
            'id': station_from.id,
            'codes': {'express': 'real_from'}
        }),
        'stationTo': has_entries({
            'id': station_to.id,
            'codes': {'express': 'real_to'}
        }),
        'company': has_entries('ufsTitle', 'ФПК'),
        'provider': 'P1',
    }))

    assert len(trainslist_mock.latest_requests) == 1
    assert trainslist_mock.last_request.parsed_body['Origin'] == 'pseudo_from'
    assert trainslist_mock.last_request.parsed_body['Destination'] == 'real_to'
