# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

import pytest

from travel.avia.library.python.tester.factories import create_country, create_settlement, create_station, create_thread
from travel.avia.library.python.common.models.geo import CityMajority
from travel.avia.library.python.common.models.transport import TransportType

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestScheduleHandler(TestApiHandler):
    def setUp(self):
        super(TestScheduleHandler, self).setUp()

        self.country = create_country(title='TEST-COUNTRY')

        self.some_settlement = create_settlement(
            id=1001,
            _geo_id=1001,
            title=u'Молот',
            country=self.country,
            majority=CityMajority.objects.get(id=CityMajority.CAPITAL_ID)
        )
        self.station_from = create_station(
            settlement=self.some_settlement, id=101, t_type=TransportType.PLANE_ID,
            title='airport Rock'
        )

        self.another_settlement = create_settlement(
            id=1002,
            _geo_id=1002,
            title=u'Наковальня',
            country=self.country,
            majority=CityMajority.objects.get(id=CityMajority.CAPITAL_ID)
        )
        self.station_to = create_station(
            settlement=self.another_settlement, id=102, t_type=TransportType.PLANE_ID,
            title='airport HardPlace'
        )
        create_thread(
            uid=1,
            t_type=TransportType.PLANE_ID,
            schedule_v1=[
                [None, 0, self.station_from],
                [10, None, self.station_to],
            ],
            __={'calculate_noderoute': True}
        )

    @pytest.mark.dbuser
    def test_basic_scenario(self):
        payload = [{
            'name': 'schedule',
            'params': {
                'fromPointKey': 'c{id}'.format(id=self.some_settlement.id),
                'toPointKey': 'c{id}'.format(id=self.another_settlement.id),
                'leftDate': '2019-07-03',
                'rightDate': '2019-07-09'
            },
            'fields': ['hasRoutes']
        }]

        data = self.api_data(payload)

        assert data == {
            'status': 'success',
            'data': [{
                'hasRoutes': True
            }]
        }

    @pytest.mark.dbuser
    def test_route_from_station_to_settlement(self):
        payload = [{
            'name': 'schedule',
            'params': {
                'fromPointKey': 's{id}'.format(id=self.station_from.id),
                'toPointKey': 'c{id}'.format(id=self.another_settlement.id),
                'leftDate': '2019-07-03',
                'rightDate': '2019-07-09'
            },
            'fields': ['hasRoutes']
        }]

        data = self.api_data(payload)

        assert data == {
            'status': 'success',
            'data': [{
                'hasRoutes': True
            }]
        }

    @pytest.mark.dbuser
    def test_route_from_settltment_to_station(self):
        payload = [{
            'name': 'schedule',
            'params': {
                'fromPointKey': 'c{id}'.format(id=self.some_settlement.id),
                'toPointKey': 's{id}'.format(id=self.station_to.id),
                'leftDate': '2019-07-03',
                'rightDate': '2019-07-09'
            },
            'fields': ['hasRoutes']
        }]

        data = self.api_data(payload)

        assert data == {
            'status': 'success',
            'data': [{
                'hasRoutes': True
            }]
        }

    @pytest.mark.dbuser
    def test_from_settlement_equals_to_means_no_routes(self):
        payload = [{
            'name': 'schedule',
            'params': {
                'fromPointKey': 'c{id}'.format(id=self.some_settlement.id),
                'toPointKey': 'c{id}'.format(id=self.some_settlement.id),
                'leftDate': '2019-07-03',
                'rightDate': '2019-07-09'
            },
            'fields': ['hasRoutes']
        }]

        data = self.api_data(payload)

        assert data == {
            'status': 'success',
            'data': [{
                'hasRoutes': False
            }]
        }

    @pytest.mark.dbuser
    def test_from_station_equals_to_means_no_routes(self):
        payload = [{
            'name': 'schedule',
            'params': {
                'fromPointKey': 's{id}'.format(id=self.station_from.id),
                'toPointKey': 's{id}'.format(id=self.station_from.id),
                'leftDate': '2019-07-03',
                'rightDate': '2019-07-09'
            },
            'fields': ['hasRoutes']
        }]

        data = self.api_data(payload)

        assert data == {
            'status': 'success',
            'data': [{
                'hasRoutes': False
            }]
        }
