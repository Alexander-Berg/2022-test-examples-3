# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from collections import namedtuple
from concurrent.futures import Future
from datetime import datetime

import hamcrest
import mock
import pytest
import pytz
from django.conf import settings
from django.test.client import Client

from common.apps.train.models import Facility
from common.models.currency import Price
from common.models.geo import Country
from common.tester.factories import create_deluxe_train, create_settlement, create_station
from common.tester.transaction_context import transaction_fixture
from travel.rasp.wizards.train_wizard_api.lib.facility_provider import facility_provider
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import (
    Place, TariffDirectionInfo, TariffDirectionUpdatedInfo, TariffDirectionUpdatedInfoRecord
)
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.train_info_provider import TrainInfoModel
import travel.rasp.wizards.train_wizard_api.lib.storage_timed_execute as storage_timed_execute
from travel.rasp.wizards.train_wizard_api.views import direction
from travel.rasp.wizards.wizard_lib.cache import Translations


@pytest.fixture
def m_find_tdi():
    with mock.patch.object(direction.tariff_direction_info_provider, 'find', autospec=True) as m_find_tdi:
        yield m_find_tdi


@pytest.fixture
def m_find_train_info_provider():
    with mock.patch.object(direction.train_info_provider, 'find', autospec=True) as m_find_train_info_provider:
        yield m_find_train_info_provider


@pytest.fixture
def m_find_segments():
    with mock.patch.object(direction.schedule_cache, 'find_segments', autospec=True) as m_find_segments:
        yield m_find_segments


@pytest.fixture
def m_timed_executor():
    with mock.patch.object(storage_timed_execute, 'wait_for_future_and_build_info', autospec=True) as m_timed_executor:
        yield m_timed_executor


_DefaultQuery = namedtuple('_DefaultQuery', 'query settlement departure_station arrival_station')


@pytest.fixture
@transaction_fixture
def default_query(request, fixed_now):
    settlement = create_settlement(title='some_settlement')
    departure_station = create_station(title='some_departure_station', settlement=settlement, country=Country.RUSSIA_ID)
    arrival_station = create_station(title='some_arrival_station', settlement=settlement, country=Country.RUSSIA_ID)
    return _DefaultQuery(
        query={
            'departure_point_key': departure_station.point_key,
            'arrival_point_key': arrival_station.point_key,
            'departure_date': '2000-01-01',
            'order_by': 'departure',
            'tld': 'ru'
        },
        settlement=settlement,
        departure_station=departure_station,
        arrival_station=arrival_station
    )


@pytest.mark.dbuser
def test_can_not_find_segments(m_find_train_info_provider, m_find_tdi, m_find_segments, default_query):
    empty_tuple_future = Future()
    empty_tuple_future.set_result(())
    m_find_train_info_provider.return_value = empty_tuple_future, {}
    m_find_tdi.return_value = empty_tuple_future, {}
    m_find_segments.return_value = ()
    response = Client().get('/searcher/api/open_direction/', dict(default_query.query))
    assert response.status_code == 204


@pytest.mark.dbuser
@pytest.mark.parametrize('segments_count, expected', (
    (1, {'is_the_fastest': [False], 'is_the_cheapest': [False]}),
    (2, {'is_the_fastest': [True, False], 'is_the_cheapest': [True, False]}),
))
def test_format(segments_count, expected, m_find_train_info_provider, m_find_tdi, m_timed_executor, m_find_segments, default_query):
    facility_provider.build_cache()
    query = default_query.query
    query['facility'] = 'WIFI'
    arrival_station = default_query.arrival_station
    departure_station = default_query.departure_station
    deluxe_train = create_deluxe_train(numbers='some_number', title_ru='some_deluxe_train', title='some_deluxe_train',
                                       deluxe=True)

    m_find_train_info_provider.return_value = 'some_future', {}
    m_find_tdi.return_value = 'some_future', {}
    train_info_result = (
        TrainInfoModel(
            departure_at=datetime(2000, 1, 1, 3, 4),
            number='some_number',
            electronic_ticket=False,
            facilities_ids=[
                Facility.WIFI_ID, Facility.EATING_ID
            ],
            updated_at=datetime(2000, 1, 1)
        ),
    )
    tdi_info_result = (
        (
            TariffDirectionInfo(
                arrival_dt='2000-06-07T12:09:00+04:00',
                arrival_station_id=arrival_station.id,
                departure_dt='2000-01-01T06:04:00+03:00',
                departure_station_id=departure_station.id,
                number="some_number",
                title_dict={},
                electronic_ticket=False,
                places=(Place(
                    coach_type='cool_type',
                    count=3,
                    max_seats_in_the_same_car=2,
                    price=Price(100, 'RUB'),
                    price_details={
                        'any_key': 'any_value',
                        'service_price': '100',
                        'ticket_price': '500',
                        'fee': '50',
                        'several_prices': False,
                    },
                    service_class='2Л',
                ),),
                broken_classes={'bad_type': [9, 10]},
                display_number='какой-то номер',
                has_dynamic_pricing=False,
                is_suburban=True,
                coach_owners=['МВФ'],
                two_storey=False,
                first_country_code='UA',
                last_country_code='RU',
                provider='P1',
                raw_train_name='Жигули',
            ),
        ), TariffDirectionUpdatedInfo((TariffDirectionUpdatedInfoRecord(
            datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
            datetime(2005, 6, 7, 8, 9, tzinfo=pytz.UTC),
            datetime(2100, 1, 2, 3, 4, tzinfo=pytz.UTC)
        ),))
    )
    m_timed_executor.side_effect = [tdi_info_result, train_info_result]
    one_segment = mock.Mock(**{
        'thread.number': 'some_number',
        'thread.title': Translations(**{lang: 'title' for lang in settings.MODEL_LANGUAGES}),
        'thread.type.code': 'through',
        'departure_dt': datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
        'departure_station': departure_station,
        'arrival_dt': datetime(2000, 6, 7, 8, 9, tzinfo=pytz.UTC),
        'arrival_station': arrival_station,
    })
    m_find_segments.return_value = [one_segment]*segments_count

    response = Client().get('/searcher/api/open_direction/', dict(query))
    assert response.status_code == 200

    expected_search_path = '/some_departure_station--some_arrival_station/?when=2000-01-01'
    expected_order_query = (
        '?fromName=some_departure_station'
        '&fromId={departure_point}'
        '&toName=some_arrival_station'
        '&toId={arrival_point}'
        '&when=2000-01-01'
        '&transportType=train&number=some_number&time=06:04'.format(
            departure_point=departure_station.point_key,
            arrival_point=arrival_station.point_key
        ) +
        '&provider=P1'
    )
    expected_segments = [{
        'arrival': {
            'station': {
                'key': arrival_station.point_key,
                'title': 'some_arrival_station'
            },
            'settlement': {
                'key': default_query.settlement.point_key,
                'title': 'some_settlement'
            },
            'local_datetime': {
                'timezone': 'Europe/Moscow',
                'value': '2000-06-07T12:09:00+04:00'
            }
        },
        'departure': {
            'station': {
                'key': departure_station.point_key,
                'title': 'some_departure_station'
            },
            'settlement': {
                'key': default_query.settlement.point_key,
                'title': 'some_settlement'
            },
            'local_datetime': {
                'timezone': 'Europe/Moscow',
                'value': '2000-01-01T06:04:00+03:00'
            }
        },
        'duration': 227825.0,
        'facilities': ['WIFI', 'EAT'],
        'is_the_fastest': expected['is_the_fastest'][i],
        'is_the_cheapest': expected['is_the_cheapest'][i],
        'minimum_price': {'currency': 'RUB', 'value': 100},
        'order_touch_url': 'https://travel.yandex.ru/trains/order/' + expected_order_query,
        'order_url': 'https://travel.yandex.ru/trains/order/' + expected_order_query,
        'places': {
            'records': [
                {
                    'count': 3,
                    'max_seats_in_the_same_car': 2,
                    'price': {
                        'currency': 'RUB', 'value': 100
                    },
                    'price_details': {
                        'any_key': 'any_value',
                        'service_price': '100',
                        'ticket_price': '500',
                        'fee': '50',
                        'several_prices': False,
                    },
                    'coach_type': 'cool_type',
                    'service_class': '2Л',
                }
            ],
            'electronic_ticket': False,
            'updated_at': {
                'timezone': 'UTC',
                'value': '2100-01-02T03:04:00+00:00'
            }
        },
        'broken_classes': {'bad_type': [9, 10]},
        'train': {
            'brand': {
                'is_deluxe': True,
                'id': deluxe_train.id,
                'is_high_speed': False,
                'title': 'some_deluxe_train',
                'short_title': 'фирменный «some_deluxe_train»',
            },
            'number': 'some_number',
            'title': 'title',
            'display_number': 'какой-то номер',
            'has_dynamic_pricing': False,
            'two_storey': False,
            'is_suburban': True,
            'coach_owners': ['МВФ'],
            'thread_type': 'through',
            'first_country_code': 'UA',
            'last_country_code': 'RU',
            'provider': 'P1',
            'raw_train_name': 'Жигули',
            't_subtype_id': None,
        },
    } for i in xrange(segments_count)]

    hamcrest.assert_that(response.json(), hamcrest.has_entries({
        'found_departure_date': '2000-01-01',
        'query': {
            'departure_point': {
                'key': departure_station.point_key,
                'title': 'some_departure_station'
            },
            'original_departure_point': None,
            'departure_date': '2000-01-01',
            'order_by': 'departure',
            'arrival_point': {
                'key': arrival_station.point_key,
                'title': 'some_arrival_station'
            },
            'original_arrival_point': None,
            'language': 'ru'
        },
        'segments': expected_segments,
        'search_touch_url': 'https://travel.yandex.ru/trains' + expected_search_path,
        'search_url': 'https://travel.yandex.ru/trains' + expected_search_path,
        'filters': {
            'place_count': [
                {'available': True, 'selected': False, 'value': 1},
                {'available': True, 'selected': False, 'value': 2},
                {'available': False, 'selected': False, 'value': 3},
                {'available': False, 'selected': False, 'value': 4}],
            'price': [
                {'available': True, 'selected': False, 'value': '-1000'},
                {'available': False, 'selected': False, 'value': '1000-2000'},
                {'available': False, 'selected': False, 'value': '2000-3000'},
                {'available': False, 'selected': False, 'value': '3000-'}
            ],
            'arrival_time': [
                {'available': False, 'selected': False, 'value': '00:00-06:00'},
                {'available': False, 'selected': False, 'value': '06:00-12:00'},
                {'available': True, 'selected': False, 'value': '12:00-18:00'},
                {'available': False, 'selected': False, 'value': '18:00-24:00'}
            ],
            'brand': [
                {
                    'available': True,
                    'selected': False,
                    'minimum_price': {'currency': 'RUB', 'value': 100},
                    'value': deluxe_train.id,
                    'title': 'some_deluxe_train',
                    'is_high_speed': False,
                }
            ],
            'departure_time': [
                {'available': False, 'selected': False, 'value': '00:00-06:00'},
                {'available': True, 'selected': False, 'value': '06:00-12:00'},
                {'available': False, 'selected': False, 'value': '12:00-18:00'},
                {'available': False, 'selected': False, 'value': '18:00-24:00'}],
            'coach_type': [
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'common'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'compartment'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'platzkarte'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'sitting'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'soft'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'suite'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'unknown'}
            ],
            'facility': [
                {'available': False,
                 'selected': False,
                 'value': 'BED'},
                {'available': False,
                 'selected': False,
                 'value': 'COND'},
                {'available': True,
                 'selected': False,
                 'value': 'EAT'},
                {'available': False,
                 'selected': False,
                 'value': 'PAP'},
                {'available': False,
                 'selected': False,
                 'value': 'SAN'},
                {'available': False,
                 'selected': False,
                 'value': 'TRAN'},
                {'available': False,
                 'selected': False,
                 'value': 'TV'},
                {'available': True,
                 'selected': True,
                 'value': 'WIFI'},
                {'available': False,
                 'selected': False,
                 'value': 'nearToilet'},
                {'available': False,
                 'selected': False,
                 'value': 'side'},
                {'available': False,
                 'selected': False,
                 'value': 'upper'}
            ]
        }
    }))
