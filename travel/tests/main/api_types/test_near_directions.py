# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from datetime import date, timedelta

from mock import patch

from travel.avia.library.python.avia_data.models import AviaDirectionNational
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import (
    create_country, create_min_price, create_settlement, create_station, get_model_factory,
)

from travel.avia.backend.tests.main.api_test import TestApiHandler
from travel.avia.backend.main.api_types.near_directions import get_settlements_within, settlement_geo_index
from travel.avia.backend.repository.settlement import (
    settlement_repository, translated_title_repository,
    environment, SettlementGeoIndex, SettlementRepository
)
from travel.avia.backend.repository import settlement


def create_settlement_with_airport(*args, **kwargs):
    s = create_settlement(type_choices='plane', **kwargs)
    create_station(settlement=s, t_type=TransportType.PLANE_ID)

    return s


class TestNearDirectionsHandler(TestApiHandler):
    def setUp(self):
        super(TestNearDirectionsHandler, self).setUp()

        self.from_city = create_settlement(latitude=56.838607, longitude=60.605514, iata='TFC')
        self.to_city = create_settlement(latitude=50.079079, longitude=14.433214, iata='TTC')

        self.settlement_repository_backup = settlement_repository
        self.settlement_geo_index_backup = settlement_geo_index
        settlement.settlement_repository = SettlementRepository(
            translated_title_repository, environment
        )
        settlement.settlement_geo_index = SettlementGeoIndex(
            settlement_repository
        )

    def tearDown(self):
        settlement.settlement_repository = self.settlement_repository_backup
        settlement.settlement_geo_index_backup = self.settlement_geo_index_backup

    def test_unknown_from_point(self):
        unknown_id = max(self.from_city.id, self.to_city.id) + 1
        payload = {
            'name': 'nearDirections',
            'params': {
                'fromKey': 'c{}'.format(unknown_id),
                'toKey': self.to_city.point_key,
                'distance': 250,
                'when': (date.today() + timedelta(days=1)).isoformat(),
                'return_date': (date.today() + timedelta(days=10)).isoformat()
            },
            'fields': ['fromCity', {
                'name': 'toCity',
                'fields': ['code']
            }, 'price']
        }

        data = self.api_data(payload)
        response = data['data'][0]

        assert response is None

    def test_unknown_to_point(self):
        unknown_id = max(self.from_city.id, self.to_city.id) + 1
        payload = {
            'name': 'nearDirections',
            'params': {
                'fromKey': self.from_city.point_key,
                'toKey': 'c{}'.format(unknown_id),
                'distance': 250,
                'when': (date.today() + timedelta(days=1)).isoformat(),
                'return_date': (date.today() + timedelta(days=10)).isoformat()
            },
            'fields': ['fromCity', {
                'name': 'toCity',
                'fields': ['code']
            }, 'price']
        }

        data = self.api_data(payload)
        response = data['data'][0]

        assert response is None

    def test_without_coordinate(self):
        def request(from_point_key, to_point_key):
            payload = {
                'name': 'nearDirections',
                'params': {
                    'fromKey': from_point_key,
                    'toKey': to_point_key,
                    'distance': 250,
                    'when': (date.today() + timedelta(days=1)).isoformat(),
                    'return_date': (date.today() + timedelta(days=10)).isoformat()
                },
                'fields': ['fromCity', {
                    'name': 'toCity',
                    'fields': ['code']
                }, 'price']
            }

            data = self.api_data(payload)
            return data['data'][0]

        point_without_coordinate = create_settlement(latitude=None, longitude=None)
        assert request(self.from_city.point_key, point_without_coordinate.point_key) is None
        assert request(point_without_coordinate.point_key, self.from_city.point_key) is None

    def test_simple(self):
        fixtures = [
            {'latitude': 51.108249, 'longitude': 17.026901, 'iata': 'PED'},
            {'latitude': 51.053628, 'longitude': 13.740801, 'iata': 'DRS'},
            {'latitude': 50.228358, 'longitude': 12.864962, 'iata': 'KLV'},
        ]

        for fixture in fixtures:
            s = create_settlement_with_airport(**fixture)
            AviaDirectionNational.objects.create(
                departure_settlement=self.from_city, arrival_settlement=s
            )

            create_min_price(
                departure_settlement=self.from_city,
                arrival_settlement=s,
                price=100,
                passengers='1_0_0'
            )

        payload = {
            'name': 'nearDirections',
            'params': {
                'fromKey': self.from_city.point_key,
                'toKey': self.to_city.point_key,
                'distance': 250,
                'when': (date.today() + timedelta(days=1)).isoformat(),
                'return_date': (date.today() + timedelta(days=10)).isoformat()
            },
            'fields': ['fromCity', {
                'name': 'toCity',
                'fields': ['code']
            }, 'price']
        }

        settlement_repository.pre_cache()
        settlement_geo_index.pre_cache()

        data = self.api_data(payload)

        response = data['data'][0]

        from_city = {
            'id': int(self.from_city.id),
            'code': 'TFC',
            'title': self.from_city.L_title()
        }

        price = {
            'baseValue': 100,
            'currency': 'RUR',
            'roughly': True,
            'value': 100,
        }

        expected_defaults = {
            'fromCity': from_city,
            'price': price
        }
        expected = [
            dict(expected_defaults, toCity={'code': code})
            for code in ('KLV', 'DRS', 'PED')
        ]

        self.assertItemsEqual(expected, response)

    def test_get_settlements_within(self):
        from_settlement = create_settlement(id=1)
        to_settlement = create_settlement(id=2)
        near_settlement = create_settlement(id=3)
        create_avia_direction = get_model_factory(AviaDirectionNational)
        create_avia_direction(
            departure_settlement=from_settlement,
            arrival_settlement=near_settlement,
            national_version='ru'
        )

        # basic positive test
        with patch.object(settlement_geo_index, 'get_nearest', return_value=[near_settlement]):
            actual = get_settlements_within(to_settlement, from_settlement, 100, 'ru')
        self.assertItemsEqual([near_settlement], actual)

        # test that country can be banned
        excluded_country = create_country(code='ZZ')
        near_settlement_ua = create_settlement(id=4, country=excluded_country)
        with patch.object(settlement_geo_index, 'get_nearest', return_value=[near_settlement_ua, near_settlement]):
            actual = get_settlements_within(to_settlement, from_settlement, 100, 'ru')
        self.assertItemsEqual([near_settlement], actual)
