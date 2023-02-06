# coding: utf8

from django.test.client import Client

from common.tester.testcase import TestCase

from travel.rasp.export.tests.v3.factories import create_station
from travel.rasp.export.tests.v3.helpers import api_get_json


class TestStation(TestCase):
    def setUp(self):
        self.client = Client()
        self.esr_code = '123'
        self.stops_data = [
            {
                'name': u'Станция {}'.format(i),
                'lang': 'ru-RU',
                'distance': i * 100.0,
                'color': '#fffff{}'.format(i),
                'coords': {
                    'latitude': float(i),
                    'longitude': float(i + 3),
                },
            }
            for i in range(1, 3)
        ]

        create_station(
            __={
                'codes': {'esr': self.esr_code},
                'phones': [
                    {'phone': '+79121111111'},
                    {'phone': '+79121111112'},
                ],
            },
            address=u'Ленина 13, 7',
            latitude=1.0,
            longitude=2.0,
        )

        response = api_get_json('/v3/suburban/station/{}'.format(self.esr_code))

        assert response['address'] == u'Ленина 13, 7'
        assert response['latitude'] == 1.0
        assert response['longitude'] == 2.0

        phones = sorted(response['phones'])
        assert phones == ['+79121111111', '+79121111112']

        assert response.get('metro') is None


class TestStationTypes(TestCase):
    def test_can_get_types(self):
        response = api_get_json('/v3/suburban/station/types')

        station_types = response['station_types']
        assert len(station_types) != 0

        for station_type in station_types:
            assert 'id' in station_type
            assert station_type['name'] is not None
