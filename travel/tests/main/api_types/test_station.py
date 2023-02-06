# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_settlement, create_station

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestStationHandler(TestApiHandler):
    # Когда понадобится - добавить params тесты

    def setUp(self):
        super(TestStationHandler, self).setUp()

        self.station = create_station(
            title='airport',
            popular_title='airport'
        )

        # случай редкий =>  в фабрику заносить не буду
        self.station.new_L_title.ru_accusative = 'airport_accusative'
        self.station.new_L_title.save()

    def test_defaults(self):
        payload = {
            'name': 'station',
            'params': {
                'id': self.station.id
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': int(self.station.id),
            'key': 's' + str(self.station.id),
            'title': 'airport',
        })

    def test_all_fields(self):
        payload = {
            'name': 'station',
            'params': {
                'id': self.station.id
            },
            'fields': ['id', 'key', 'title', 'popular_title', 'phrase_to']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': int(self.station.id),
            'key': 's' + str(self.station.id),
            'title': 'airport',
            'popularTitle': 'airport',
            # неразрывный пробел
            'phraseTo': 'airport_accusative',
        })

    def test_all_fields_for_en(self):
        payload = {
            'name': 'station',
            'params': {
                'id': self.station.id
            },
            'fields': ['id', 'key', 'title', 'popular_title', 'phrase_to']
        }

        data = self.api_data(payload, {'lang': 'en'})

        assert data == self.wrap_expect({
            'id': int(self.station.id),
            'key': 's' + str(self.station.id),
            'title': 'airport',
            'popularTitle': 'airport',
            # неразрывный пробел
            'phraseTo': 'airport',
        })

    def test_without_county(self):
        payload = {
            'name': 'station',
            'params': {
                'id': self.station.id
            },
            'fields': [
                'id',
                'key',
                'title',
                {
                    'name': 'country',
                    'fields': [
                        'id',
                        'title',
                    ],
                },
            ]
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': int(self.station.id),
            'key': 's' + str(self.station.id),
            'title': 'airport',
            'country': None,
        })


class TestStationsHandler(TestApiHandler):
    def setUp(self):
        super(TestStationsHandler, self).setUp()

        settlement = create_settlement()
        self.settlement_id = settlement.id

        settlement_empty = create_settlement()
        self.settlement_empty_id = settlement_empty.id

        # Аэропорты
        create_station(
            settlement=settlement, id=1, t_type=TransportType.PLANE_ID,
            title='one station',
            type_choices='plane,aeroex'
        )
        create_station(
            settlement=settlement, id=2, t_type=TransportType.PLANE_ID,
            hidden=True
        )
        create_station(
            settlement=settlement, id=3, t_type=TransportType.PLANE_ID,
            majority='not_in_tablo'
        )
        st5 = create_station(
            id=4, t_type=TransportType.PLANE_ID, title='s2s station'
        )
        settlement.station2settlement_set.create(station=st5)

        # Поезда
        create_station(
            settlement=settlement, id=11, t_type=TransportType.TRAIN_ID,
            title='one station train'
        )
        create_station(
            settlement=settlement, id=12, t_type=TransportType.TRAIN_ID,
            hidden=True
        )
        create_station(
            settlement=settlement, id=13, t_type=TransportType.TRAIN_ID,
            majority='not_in_tablo'
        )
        st14 = create_station(
            id=14, t_type=TransportType.TRAIN_ID, title='s2s station train'
        )
        settlement.station2settlement_set.create(station=st14)

    def test_empty_params(self):
        payload = {
            'name': 'stations',
            'params': {
                'ttype': 'airport'
            }
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'params are not valid',
            'description': {'settlement_id': ['id or key required']}
        })

    def test_ttype_params(self):
        VALID_TTYPES = ['airport', 'train', 'aeroexpress']

        for ttype in VALID_TTYPES:
            payload = {
                'name': 'stations',
                'params': {
                    'settlement_id': self.settlement_id,
                    'ttype': ttype
                }
            }

            data = self.api_data(payload)

            assert data['status'] == 'success'

    def test_wrongttype_param(self):
        payload = {
            'name': 'stations',
            'params': {
                'settlement_id': self.settlement_id,
                'ttype': 'wrong'
            }
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'params are not valid',
            'description': {'ttype': ['Not a valid choice.']}
        })

    def test_airports(self):
        payload = {
            'name': 'stations',
            'params': {
                'settlement_id': self.settlement_id,
                'ttype': 'airport'
            },
            'fields': ['id', 'title']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect([{
            'id': 1,
            'title': 'one station'
        }, {
            'id': 4,
            'title': 's2s station'
        }])

    def test_trains(self):
        payload = {
            'name': 'stations',
            'params': {
                'settlement_id': self.settlement_id,
                'ttype': 'train'
            },
            'fields': ['id', 'title']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect([{
            'id': 11,
            'title': 'one station train'
        }, {
            'id': 14,
            'title': 's2s station train'
        }])

    def test_aeroexpresses(self):
        payload = {
            'name': 'stations',
            'params': {
                'settlement_id': self.settlement_id,
                'ttype': 'aeroexpress'
            },
            'fields': ['id', 'title']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect([{
            'id': 1,
            'title': 'one station'
        }])

    def test_empty_airports(self):
        payload = {
            'name': 'stations',
            'params': {
                'settlement_id': self.settlement_empty_id,
                'ttype': 'airport'
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect([])
