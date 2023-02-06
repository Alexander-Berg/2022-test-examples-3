# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

import mock
import pytest

from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_settlement, create_station

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestGeoLookupHandler(TestApiHandler):
    def setUp(self):
        super(TestGeoLookupHandler, self).setUp()

        self.settlement = create_settlement(
            id=54,
            _geo_id=54,
            title='Екатеринбург',
            iata='SVX'
        )

        create_station(
            settlement=self.settlement, id=1, t_type=TransportType.PLANE_ID
        )

        # Из-за кэширования города нужно создавать тут
        LED = create_settlement(id=2, title='Питер', iata='LED')
        BKK = create_settlement(id=3, title='Бангкок', iata='BKK')
        create_station(hidden=False, majority=1, t_type=TransportType.PLANE_ID, settlement=LED)
        create_station(hidden=False, majority=1, t_type=TransportType.PLANE_ID, settlement=BKK)

    def test_defaults(self):
        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54
            }
        }

        data = self.api_data(payload)

        ekb_default_json = {
            'id': self.settlement.id,
            'title': self.settlement.title,
            'code': self.settlement.iata,
        }

        assert data == self.wrap_expect({
            'clientCity': ekb_default_json,
            'searchCity': ekb_default_json,
            'fromSettlement': ekb_default_json,
            'fromError': None,
        })

    @pytest.skip('This test uses geobase by http')
    def test_defaults_no_geoid(self):
        s = Settlement.objects.get(id=213)
        s._geo_id = None
        s.save()

        payload = {
            'name': 'geoLookup'
        }

        data = self.api_data(payload)

        moscow_default_json = {
            'id': s.id,
            'title': s.title,
            'code': s.iata,
        }

        assert data == self.wrap_expect({
            'clientCity': moscow_default_json,
            'searchCity': moscow_default_json,
            'fromSettlement': moscow_default_json,
            'fromError': None,
        })

    @pytest.skip('This test uses geobase by http')
    def test_default_no_airports(self):
        s = create_settlement(id=321, _geo_id=321, title='Пышма')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': s.id
            }
        }

        data = self.api_data(payload)

        moscow_default_json = {
            'id': 213,
            'title': 'Москва',
            'code': 'MOW',
        }

        assert data == self.wrap_expect({
            'clientCity': {
                'id': s.id,
                'title': s.title,
                'code': None
            },
            'searchCity': moscow_default_json,
            'fromSettlement': moscow_default_json,
            'fromError': None,
        })

    def test_search_from_point(self):
        s = create_settlement(id=2, title='Питер', iata='LED')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromId': s.point_key
            },
            'fields': ['fromSettlement']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'fromSettlement': {
                'id': s.id,
                'title': s.title,
                'code': s.iata,
            }
        })

    def test_correct_search_id(self):
        s1 = create_settlement(id=2, title='Питер')
        s2 = create_settlement(id=3, title='Бангкок')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromId': s1.point_key,
                'toId': s2.point_key,
            },
            'fields': ['fromSettlement', 'toSettlement']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'fromSettlement': {
                'id': s1.id,
                'title': s1.title,
                'code': None,
            },
            'toSettlement': {
                'id': s2.id,
                'title': s2.title,
                'code': None,
            }
        })

    # стоит добавить тест поиска по name, но мокать нужно больше
    # а вероятность там сломаться не большая. Пока отложим.

    def test_empty_to_search(self):
        s = create_settlement(id=2, title='Питер', iata='LED')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromId': s.point_key
            },
            'fields': ['fromSettlement', 'toSettlement', 'toError']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'fromSettlement': {
                'id': s.id,
                'title': s.title,
                'code': s.iata,
            },
            'toSettlement': None,
            'toError': 'does_not_exist'
        })

    def test_station_search(self):
        s1 = create_station(id=2, title='Питер')
        s2 = create_station(id=3, title='Бангкок')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromId': s1.point_key,
                'toId': s2.point_key
            },
            'fields': ['fromStation', 'toStation']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'fromStation': {
                'id': s1.id,
                'key': 's' + str(s1.id),
                'title': s1.title,
            },
            'toStation': {
                'id': s2.id,
                'key': 's' + str(s2.id),
                'title': s2.title,
            }
        })

    def test_code_from(self):
        s = create_settlement(id=2, title='Питер', iata='LED')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromCode': s.iata
            },
            'fields': ['fromSettlement']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'fromSettlement': {
                'id': s.id,
                'title': s.title,
                'code': s.iata,
            }
        })

    def test_correct_search_codes(self):
        LED = create_settlement(id=2, title='Питер', iata='LED')
        BKK = create_settlement(id=3, title='Бангкок', iata='BKK')
        # создаем аэропорты, так как искать до городов без аэропорта нельзя
        create_station(hidden=False, majority=1, t_type=TransportType.PLANE_ID, settlement=LED)
        create_station(hidden=False, majority=1, t_type=TransportType.PLANE_ID, settlement=BKK)

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromCode': LED.iata,
                'toCode': BKK.iata
            },
            'fields': ['fromSettlement', 'toSettlement']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'fromSettlement': {
                'id': LED.id,
                'title': LED.title,
                'code': LED.iata,
            },
            'toSettlement': {
                'id': BKK.id,
                'title': BKK.title,
                'code': BKK.iata,
            }
        })

    def test_id_override_codes(self):
        s1 = create_settlement(id=2, title='Питер', iata='LED')
        s2 = create_settlement(id=3, title='Бангкок', iata='BKK')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromId': s1.point_key,
                'toId': s2.point_key,
                'fromCode': s1.iata,
                'toCode': s2.iata
            },
            'fields': ['fromSettlement', 'toSettlement']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'fromSettlement': {
                'id': s1.id,
                'title': s1.title,
                'code': s1.iata,
            },
            'toSettlement': {
                'id': s2.id,
                'title': s2.title,
                'code': s2.iata,
            }
        })

    def test_search_city_by_params(self):
        create_settlement(id=2, title='Питер', iata='LED')

        payload = {
            'name': 'geoLookup',
            'params': {
                'geoId': 54,
                'fromId': 'c2',
                'fromCode': '2',
            },
            'fields': ['searchCity']
        }

        data = self.api_data(payload)

        assert data['data'][0]['searchCity']['id'] == 2

    def test_from_adjustments(self):
        from travel.avia.library.python.geosearch.views.pointtopoint import PointList

        s1 = create_settlement(id=10, title='Сидней', iata='SYD')
        s2 = create_settlement(id=11, title='Сидней', iata='YQY')

        return_value = PointList(s1, variants=[s1, s2], term='Сидней')

        with mock.patch('travel.avia.library.python.geosearch.views.point.PointSearch.find_point',
                        return_value=return_value):
            payload = {
                'name': 'geoLookup',
                'params': {
                    'geoId': 54,
                    'fromName': 'Сидней',
                },
                'fields': ['fromAdjustments']
            }

            data = self.api_data(payload)

            assert data == self.wrap_expect({
                'fromAdjustments': [{
                    'code': 'YQY',
                    'title': 'Сидней',
                    'country': None,
                    'id': 11,
                    'key': 'c11',
                    'ptype': 'settlement'
                }]
            })

    def test_to_adjustments(self):
        from travel.avia.library.python.geosearch.views.pointtopoint import PointList

        s1 = create_settlement(id=10, title='Сидней', iata='SYD')
        s2 = create_settlement(id=11, title='Сидней', iata='YQY')

        def side_effect(name, ttype, id):
            if name:
                return PointList(s1, variants=[s1, s2], term='Сидней')

            return PointList(self.settlement)

        with mock.patch('travel.avia.library.python.geosearch.views.point.PointSearch.find_point',
                        side_effect=side_effect):
            payload = {
                'name': 'geoLookup',
                'params': {
                    'geoId': 54,
                    'toName': 'Сидней',
                },
                'fields': ['toAdjustments']
            }

            data = self.api_data(payload)

            assert data == self.wrap_expect({
                'toAdjustments': [{
                    'code': 'YQY',
                    'title': 'Сидней',
                    'country': None,
                    'id': 11,
                    'key': 'c11',
                    'ptype': 'settlement'
                }]
            })
