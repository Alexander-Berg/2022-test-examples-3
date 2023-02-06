# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from collections import namedtuple
from datetime import datetime

import mock
import pytest
import pytz
from django.conf import settings
from django.test.client import Client

from common.models.currency import Price
from common.models.geo import Country
from common.tester.factories import create_deluxe_train, create_station
from common.tester.transaction_context import transaction_fixture
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.models.place import Place
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.models.tariff_direction_info import TariffDirectionInfo
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.models.tariff_direction_updated_info import TariffDirectionUpdatedInfo
from travel.rasp.wizards.train_wizard_api.views import legacy_direction
from travel.rasp.wizards.wizard_lib.cache import Translations


@pytest.fixture
def m_find_tdi():
    with mock.patch.object(legacy_direction.tariff_direction_info_provider, 'find', autospec=True) as m_find_tdi:
        yield m_find_tdi


@pytest.fixture
def m_find_train_info_provider():
    with mock.patch.object(legacy_direction.train_info_provider, 'find', autospec=True) as m_find_train_info_provider:
        yield m_find_train_info_provider


@pytest.fixture
def m_find_segments():
    with mock.patch.object(legacy_direction.schedule_cache, 'find_segments', autospec=True) as m_find_segments:
        yield m_find_segments


_DefaultQuery = namedtuple('_DefaultQuery', 'query departure_station arrival_station')


@pytest.fixture
@transaction_fixture
def default_query(request, fixed_now):
    departure_station = create_station(title='some_departure_station', country=Country.RUSSIA_ID)
    arrival_station = create_station(title='some_arrival_station', country=Country.RUSSIA_ID)
    return _DefaultQuery(
        query={
            'departure_point_key': departure_station.point_key,
            'arrival_point_key': arrival_station.point_key,
            'date': '2000-01-01',
            'tld': 'ru'
        },
        departure_station=departure_station,
        arrival_station=arrival_station
    )


@pytest.mark.dbuser
def test_can_not_find_segments(m_find_train_info_provider, m_find_tdi, m_find_segments, default_query):
    m_find_tdi.return_value = ((), TariffDirectionUpdatedInfo(()))
    m_find_train_info_provider.return_value = ()
    m_find_segments.return_value = ()
    response = Client().get('/searcher/api/legacy_direction/', dict(default_query.query))
    assert response.status_code == 204


@pytest.mark.dbuser
def test_format(m_find_train_info_provider, m_find_tdi, m_find_segments, default_query):
    query = default_query.query
    departure_station = default_query.departure_station
    arrival_station = default_query.arrival_station
    create_deluxe_train(numbers='some_number', title_ru='some_deluxe_train', title='some_deluxe_train')

    m_find_tdi.return_value = (
        (
            TariffDirectionInfo(
                arrival_dt='2000-06-07T12:09:00+04:00',
                arrival_station_id=arrival_station.id,
                departure_dt='2000-01-01T06:04:00+03:00',
                departure_station_id=departure_station.id,
                number="some_number",
                title_dict={},
                electronic_ticket=True,
                places=(Place(
                    coach_type='cool_type',
                    count=3,
                    max_seats_in_the_same_car=2,
                    price=Price(100, 'RUB'),
                    price_details=None,
                    service_class='2Л',
                ),),
                display_number='какой-то номер',
                has_dynamic_pricing=False,
                is_suburban=True,
                coach_owners=['МВФ'],
                two_storey=False,
                first_country_code='UA',
                last_country_code='RU',
                broken_classes={'bad_type': [9, 10]},
                provider='P1',
                raw_train_name='Жигули',
            ),
        ),
        TariffDirectionUpdatedInfo(()),
    )
    m_find_train_info_provider.return_value = ()
    m_find_segments.return_value = (
        mock.Mock(**{
            'thread.number': 'some_number',
            'thread.title': Translations(**{lang: 'title' for lang in settings.MODEL_LANGUAGES}),
            'departure_dt': datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
            'departure_station': departure_station,
            'arrival_dt': datetime(2000, 1, 2, 5, 6, tzinfo=pytz.UTC),
            'arrival_station': arrival_station,
        }),
    )

    response = Client().get('/searcher/api/legacy_direction/', dict(query))
    assert response.status_code == 200

    expected_order_query = (
        '?fromName=some_departure_station'
        '&fromId={}&toName=some_arrival_station'
        '&toId={}&when=2000-01-01'
        '&transportType=train&number=some_number&time=06:04&provider=P1'
        .format(departure_station.point_key, arrival_station.point_key)
    )
    expected_search_path = '/some_departure_station--some_arrival_station/?when=2000-01-01'
    assert response.json() == {
        'default_transport': {
            'departure_date': '2000-01-01',
            'minimum_duration': 1562.0,
            'minimum_price': {'currency': 'RUB', 'value': 100},
            'segments': [
                {
                    'arrival': '2000-01-02 08:06:00 +0300',
                    'arrival_station': 'some_arrival_station',
                    'brand': 'some_deluxe_train',
                    'departure': '2000-01-01 06:04:00 +0300',
                    'departure_station': 'some_departure_station',
                    'duration': 1562.0,
                    'from_station': 'от some_departure_station',
                    'number': 'some_number',
                    'order_touch_url': 'https://travel.yandex.ru/trains/order/' + expected_order_query,
                    'order_url': 'https://travel.yandex.ru/trains/order/' + expected_order_query,
                    'places': [{
                        'coach_type': 'cool_type',
                        'count': 3,
                        'order_touch_url':
                            'https://travel.yandex.ru/trains/order/{}&coachType=cool_type'.format(expected_order_query),
                        'order_url':
                            'https://travel.yandex.ru/trains/order/{}&coachType=cool_type'.format(expected_order_query),
                        'price': {'currency': 'RUB', 'value': 100},
                    }],
                    'title': 'title',
                    'price': {'currency': 'RUB', 'value': 100}
                }
            ],
            'total': 1,
            'touch_url': 'https://travel.yandex.ru/trains' + expected_search_path,
            'transport': 'train',
            'url': 'https://travel.yandex.ru/trains' + expected_search_path,
        },
        'path_items': [{
            'text': 'travel.yandex.ru/trains/',
            'touch_url': 'https://travel.yandex.ru/trains' + expected_search_path,
            'url': 'https://travel.yandex.ru/trains' + expected_search_path,
        }],
        'query': {
            'arrival_point': {'key': arrival_station.point_key, 'title': 'some_arrival_station'},
            'departure_date': '2000-01-01',
            'departure_point': {'key': departure_station.point_key, 'title': 'some_departure_station'},
        },
        'title': {'__hl': 'some_departure_station — some_arrival_station: билеты на поезд, 1 января'},
        'train_title': {'__hl': 'some_departure_station — some_arrival_station: билеты на поезд, 1 января'},
        'touch_url': 'https://travel.yandex.ru/trains' + expected_search_path,
        'type': 'transports_with_default',
        'url': 'https://travel.yandex.ru/trains' + expected_search_path,
    }
