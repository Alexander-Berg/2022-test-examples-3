# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import mock
import pytest
from django.test.client import Client

from common.models.currency import Price
from common.models.geo import Country
from common.tester.factories import create_deluxe_train, create_settlement, create_station
from common.tester.transaction_context import transaction_fixture
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import Place, TariffDirectionInfo
from travel.rasp.wizards.train_wizard_api.views import prices_by_directions
from travel.rasp.wizards.train_wizard_api.serialization.prices_by_directions import TariffsByDirectionQuery


API_ENDPOINT = '/searcher/api/prices_by_directions/'


@pytest.fixture
def m_find_tdi():
    with (mock.patch.object(prices_by_directions.tariff_direction_info_provider,
                            'find_tariffs_by_directions',
                            autospec=True)) as m_find_tdi:
        yield m_find_tdi


@pytest.fixture
@transaction_fixture
def default_query(request, fixed_now):
    settlement = create_settlement(title='some_settlement')
    departure_station = create_station(title='some_departure_station', settlement=settlement, country=Country.RUSSIA_ID)
    arrival_station = create_station(title='some_arrival_station', settlement=settlement, country=Country.RUSSIA_ID)
    return TariffsByDirectionQuery(
        directions=[(departure_station, arrival_station)],
        departure_date_from=datetime(2017, 1, 1, 10, 0, 0).strftime('%Y-%d-%mT%H:%M'),
        departure_date_to=datetime(2017, 1, 2, 10, 0, 0).strftime('%Y-%d-%mT%H:%M'),
        tld='r'
    )


def _prepare_query(query):
    return {
        'departure_points': [p[0].point_key for p in query.directions],
        'arrival_points': [p[1].point_key for p in query.directions],
        'departure_date_from': query.departure_date_from,
        'departure_date_to': query.departure_date_to,
        'tld': query.tld
    }


@pytest.mark.dbuser
def test_can_not_find_segments(m_find_tdi, default_query):
    m_find_tdi.return_value = []
    response = Client().get(API_ENDPOINT, _prepare_query(default_query))
    assert response.status_code == 204


@pytest.mark.dbuser
def test_format(m_find_tdi, default_query):
    departure_station, arrival_station = default_query.directions[0]
    deluxe_train = create_deluxe_train(numbers='some_number', title_ru='some_deluxe_train', title='some_deluxe_train')

    coach_owners, display_number = ['ФПК'], 'сом_нумбер'
    m_find_tdi.return_value = [
        TariffDirectionInfo(
            arrival_dt='2000-06-07T12:09:00+04:00',
            arrival_station_id=arrival_station.id,
            departure_dt='2000-01-01T06:04:00+03:00',
            departure_station_id=departure_station.id,
            number='some_number',
            display_number=display_number,
            has_dynamic_pricing=True,
            two_storey=False,
            is_suburban=True,
            coach_owners=coach_owners,
            title_dict={},
            electronic_ticket=False,
            first_country_code='UA',
            last_country_code='RU',
            places=(Place(
                coach_type='cool_type',
                count=3,
                max_seats_in_the_same_car=2,
                price=Price(100, 'RUB'),
                price_details={
                    'service_price': '100',
                    'ticket_price': '500',
                    'fee': '50',
                    'several_prices': False,
                },
                service_class='2Л',
            ),),
            broken_classes={'bad_type': [9, 10]},
            provider='P1',
            raw_train_name='Жигули',
        )
    ]

    response = Client().get(API_ENDPOINT, _prepare_query(default_query))
    assert response.status_code == 200

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

    assert response.json() == [
        {
            'arrival_dt': '2000-06-07T12:09:00+04:00',
            'arrival_station_id': int(arrival_station.id),
            'coach_owners': coach_owners,
            'departure_dt': '2000-01-01T06:04:00+03:00',
            'departure_station_id': int(departure_station.id),
            'display_number': display_number,
            'electronic_ticket': False,
            'has_dynamic_pricing': True,
            'is_suburban': True,
            'number': deluxe_train.numbers,
            'order_url': 'https://travel.yandex.ru/trains/order/' + expected_order_query,
            'places': [
                {
                    'coach_type': 'cool_type',
                    'count': 3,
                    'max_seats_in_the_same_car': 2,
                    'price': {
                        'base_value': None,
                        'currency': 'RUB',
                        'sort_value': [None, 100],
                        'value': 100
                    },
                    'price_details': {
                        'fee': '50',
                        'service_price': '100',
                        'several_prices': False,
                        'ticket_price': '500'
                    },
                    'service_class': '2Л',
                }
            ],
            'title_dict': {},
            'two_storey': False,
            'first_country_code': 'UA',
            'last_country_code': 'RU',
            'provider': 'P1',
            'raw_train_name': 'Жигули',
        }
    ]


@pytest.mark.dbuser
def test_find_batch(m_find_tdi, default_query):
    departure_station_1, arrival_station_1 = default_query.directions[0]

    settlement_2 = create_settlement(title='some_settlement_2')
    create_station(title='some_departure_station_2', settlement=settlement_2, country=Country.RUSSIA_ID)
    create_station(title='some_arrival_station_2', settlement=settlement_2, country=Country.RUSSIA_ID)

    create_deluxe_train(numbers='some_number', title_ru='some_deluxe_train', title='some_deluxe_train')

    coach_owners, display_number = ['ФПК'], 'сом_нумбер'
    m_find_tdi.return_value = [
        TariffDirectionInfo(
            arrival_dt='2000-06-07T12:09:00+04:00',
            arrival_station_id=arrival_station_1.id,
            departure_dt='2000-01-01T06:04:00+03:00',
            departure_station_id=departure_station_1.id,
            number='some_number',
            display_number=display_number,
            has_dynamic_pricing=True,
            two_storey=False,
            is_suburban=True,
            coach_owners=coach_owners,
            title_dict={},
            electronic_ticket=False,
            first_country_code='UA',
            last_country_code='RU',
            places=(Place(
                coach_type='cool_type',
                count=3,
                max_seats_in_the_same_car=2,
                price=Price(100, 'RUB'),
                price_details={
                    'service_price': '100',
                    'ticket_price': '500',
                    'fee': '50',
                    'several_prices': False,
                },
                service_class='2Л',
            ),),
            broken_classes={'bad_type': [9, 10]},
            provider='P1',
            raw_train_name='Жигули',
        )
    ]

    response = Client().get(API_ENDPOINT, _prepare_query(default_query))
    assert response.status_code == 200
    result = response.json()
    assert len(result) == 1
    assert result[0]['arrival_station_id'] == arrival_station_1.id
    assert result[0]['departure_station_id'] == departure_station_1.id
