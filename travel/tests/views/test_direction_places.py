# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from collections import namedtuple
from datetime import date

import hamcrest
import mock
import pytest
from django.test.client import Client
from django.utils import translation

from common.models.currency import Price
from common.tester.factories import create_station
from common.tester.transaction_context import transaction_fixture
from common.tester.utils.datetime import replace_now
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import (
    TariffDirectionInfo, Place, TariffDirectionUpdatedInfo
)
from travel.rasp.wizards.train_wizard_api.views.direction_places import tariff_direction_info_provider


@pytest.fixture
def m_find_tdi():
    with mock.patch.object(tariff_direction_info_provider, 'find', autospec=True) as m_find_tdi:
        yield m_find_tdi


_DefaultQuery = namedtuple('_DefaultQuery', 'query departure_station arrival_station')


@pytest.fixture
@transaction_fixture
def default_query(request):
    departure_station = create_station()
    arrival_station = create_station()
    return _DefaultQuery(
        query={'departure_point_key': departure_station.point_key,
               'arrival_point_key': arrival_station.point_key, 'tld': 'ru'},
        departure_station=departure_station,
        arrival_station=arrival_station
    )


@pytest.mark.dbuser
def test_language_activation(m_find_tdi, default_query):
    m_find_tdi.return_value = ((), TariffDirectionUpdatedInfo(()))
    response = Client().get('/searcher/api/direction_places/', dict(default_query.query, **{'language': 'uk'}))
    assert response.status_code == 200
    assert translation.get_language() == 'uk'


def create_tariff_direction_info(departure_dt, places):
    return TariffDirectionInfo(
        arrival_dt='',
        arrival_station_id=213,
        departure_dt=departure_dt,
        departure_station_id=2,
        number='',
        title_dict={},
        places=places,
        electronic_ticket=True,
        display_number='',
        has_dynamic_pricing=False,
        is_suburban=False,
        coach_owners=[],
        two_storey=False,
        first_country_code='UA',
        last_country_code='RU',
        broken_classes={},
        provider='P1',
        raw_train_name='Жигули',
    )


@pytest.mark.dbuser
@replace_now('2000-01-01')
def test_aggregation(m_find_tdi, default_query):
    m_find_tdi.return_value = (
        [
            create_tariff_direction_info(
                '2000-01-02T12:00:00+00:00',
                (
                    Place(
                        coach_type='platzkarte',
                        count=50,
                        max_seats_in_the_same_car=15,
                        price=Price(1000, 'RUB'),
                        price_details=None,
                        service_class='2Л',
                    ),
                    Place(
                        coach_type='compartment',
                        count=50,
                        max_seats_in_the_same_car=15,
                        price=Price(10000, 'RUB'),
                        price_details=None,
                        service_class='2Л',
                    ),
                )
            ),
            create_tariff_direction_info(
                '2000-01-02T13:00:00+00:00',
                (
                    Place(
                        coach_type='platzkarte',
                        count=25,
                        max_seats_in_the_same_car=25,
                        price=Price(500, 'RUB'),
                        price_details=None,
                        service_class='2Л',
                    ),
                )
            ),
            create_tariff_direction_info(
                '2000-01-02T14:00:00+00:00',
                (
                    Place(
                        coach_type='compartment',
                        count=100,
                        max_seats_in_the_same_car=55,
                        price=Price(9000, 'RUB'),
                        price_details=None,
                        service_class='2Л',
                    ),
                )
            ),
            create_tariff_direction_info(
                '2000-01-04T12:00:00+00:00',
                (
                    Place(
                        coach_type='sitting',
                        count=100,
                        max_seats_in_the_same_car=50,
                        price=Price(500, 'RUB'),
                        price_details=None,
                        service_class='2Л',
                    ),
                )
            ),
        ], TariffDirectionUpdatedInfo(()))

    response = Client().get('/searcher/api/direction_places/', default_query.query)
    assert response.status_code == 200
    hamcrest.assert_that(response.json(), hamcrest.has_entries({
        'query': hamcrest.instance_of(dict),
        'groups': hamcrest.contains(
            {
                'departure_date': '2000-01-01',
                'places': []
            },
            hamcrest.has_entries({
                'departure_date': '2000-01-02',
                'places': hamcrest.contains_inanyorder(
                    {
                        'coach_type': 'platzkarte',
                        'count': 75,
                        'max_seats_in_the_same_car': 25,
                        'minimum_price': {
                            'value': 500,
                            'currency': 'RUB'
                        }
                    },
                    {
                        'coach_type': 'compartment',
                        'count': 150,
                        'max_seats_in_the_same_car': 55,
                        'minimum_price': {
                            'value': 9000,
                            'currency': 'RUB'
                        }
                    },
                )
            }),
            {
                'departure_date': '2000-01-03',
                'places': []
            },
            {
                'departure_date': '2000-01-04',
                'places': [
                    {
                        'coach_type': 'sitting',
                        'count': 100,
                        'max_seats_in_the_same_car': 50,
                        'minimum_price': {
                            'value': 500,
                            'currency': 'RUB'
                        }
                    }
                ]
            },
            {
                'departure_date': '2000-01-05',
                'places': []
            },
        )
    }))
    m_find_tdi.assert_called_once_with(
        default_query.departure_station,
        default_query.arrival_station,
        date(2000, 1, 1),
        days=5
    )
