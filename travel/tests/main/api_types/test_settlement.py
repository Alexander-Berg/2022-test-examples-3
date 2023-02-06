# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.tester.factories import create_settlement, create_station, create_country
from travel.avia.library.python.common.models.geo import CityMajority
from travel.avia.library.python.common.models.tariffs import Setting
from travel.avia.library.python.common.models.transport import TransportType

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestSettlementHandler(TestApiHandler):
    def setUp(self):
        super(TestSettlementHandler, self).setUp()

        self.country = create_country(title='URAL')

        self.settlement = create_settlement(
            id=54,
            _geo_id=54,
            title=u'Екатеринбург',
            iata='SVX',
            sirena_id='ЕКБ',
            country=self.country,
            majority=CityMajority.objects.get(id=CityMajority.CAPITAL_ID)
        )

        # проверка плохих станций
        create_station(
            settlement=self.settlement, id=2, t_type=TransportType.PLANE_ID,
            hidden=True
        )
        create_station(
            settlement=self.settlement, id=3, t_type=TransportType.PLANE_ID,
            majority='not_in_tablo'
        )

        Setting(code='TAXI_GEOIDS', value='213,54').save()

        # для проверки станций
        settlement_w_stations = create_settlement(
            id=2,
            title=u'Питер',
            iata='LED',
        )

        create_station(
            settlement=settlement_w_stations, id=1, t_type=TransportType.PLANE_ID,
            title='airport title'
        )

        self.settlement_w_stations_id = settlement_w_stations.id

        # станции через s2s
        settlement_w_s2s = create_settlement()

        st5 = create_station(
            id=4, t_type=TransportType.PLANE_ID
        )
        settlement_w_s2s.station2settlement_set.create(station=st5)

        self.settlement_w_s2s_id = settlement_w_s2s.id

        settlement_no_taxi = create_settlement()
        self.settlement_no_taxi_id = settlement_no_taxi.id

    def test_empty_params(self):
        payload = {
            'name': 'settlement',
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)

    def test_id_override_key(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': 54,
                'key': 'c213'
            }
        }

        data = self.api_data(payload)

        assert data['data'][0]['id'] == 54

    def test_defaults(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement.id
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': self.settlement.id,
            'title': self.settlement.title,
            'code': self.settlement.iata,
        })

    def test_base_fields(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement.id
            },
            'fields': ['id', 'title', 'code', 'geo_id', 'iata_code', 'sirena_code', 'utcoffset']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': self.settlement.id,
            'geoId': self.settlement._geo_id,
            'title': self.settlement.title,
            'code': self.settlement.iata,
            'iataCode': self.settlement.iata,
            'sirenaCode': self.settlement.sirena_id,
            'utcoffset': 10800
        })

    def test_related_fields(self):
        country = create_country(title='test')

        settlement = create_settlement()
        settlement.country = country
        settlement.save()

        payload = {
            'name': 'settlement',
            'params': {
                'id': settlement.id
            },
            'fields': ['id', 'country']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': int(settlement.id),
            'country': {
                'id': country.id,
                'title': 'test',
            }
        })

    def test_no_airports(self):
        s = create_settlement()

        payload = {
            'name': 'settlement',
            'params': {
                'id': s.id
            },
            'fields': ['has_airport']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'hasAirport': False
        })

    def test_has_airports(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement_w_stations_id
            },
            'fields': ['has_airport']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'hasAirport': True
        })

    def test_has_s2s_airports(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement_w_s2s_id
            },
            'fields': ['has_airport']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'hasAirport': True
        })

    def test_taxi_no_geoid(self):
        # без geoid
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement_w_stations_id
            },
            'fields': ['has_taxi']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'hasTaxi': False
        })

    def test_no_taxi(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement_no_taxi_id
            },
            'fields': ['has_taxi']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'hasTaxi': False
        })

    def test_has_taxi(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement.id
            },
            'fields': ['has_taxi']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'hasTaxi': True
        })

    def test_stations(self):
        payload = {
            'name': 'settlement',
            'params': {
                'id': self.settlement_w_stations_id
            },
            'fields': ['airports', 'aeroexpress', 'trains']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'airports': [{
                'id': 1,
                'key': 's1',
                'title': 'airport title'
            }],
            'aeroexpress': [],
            'trains': [],
        })

    def test_get_by_iata_code(self):
        payload = {
            'name': 'settlement',
            'params': {
                'code': self.settlement.iata
            },
            'fields': ['key']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'key': self.settlement.point_key
        })

    def test_get_capital(self):
        payload = {
            'name': 'settlement',
            'params': {
                'code': self.settlement.iata
            },
            'fields': ['isCapital']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'isCapital': True
        })
