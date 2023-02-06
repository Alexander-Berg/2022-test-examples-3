# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from common.models.geo import Settlement, StationMajority, StationType
from common.tester.factories import create_station, create_settlement, create_station_majority
from travel.rasp.api_public.tests.v3 import ApiTestCase, COORDS_TO_CHECK


class TestNearestSettlement(ApiTestCase):
    def test_valid(self):
        lat, lng = 56.50, 60.35

        settlement = create_settlement(id=100500, title=u'Гондор', latitude=lat, longitude=lng)
        query_params = {
            'format': 'json',
            'lang': 'ru_RU',
            'lat': lat,
            'lng': lng,
            'distance': 50,
        }

        result = self.api_get_json('nearest_settlement', query_params)
        expected_result = {
            'title': settlement.title,
            'distance': 0.0,
            'code': 'c100500',
            'lat': lat,
            'lng': lng,
            'popular_title': None,
            'short_title': None,
            'type': 'settlement',
        }
        assert expected_result == result

    def test_coords_check(self):
        def check_call(lat, lng, status_code):
            create_settlement(latitude=lat, longitude=lng)
            query = {'lat': str(lat), 'lng': str(lng), 'distance': 10}
            assert self.api_get('nearest_settlement', query).status_code == status_code

        for lat, lng, response_code in COORDS_TO_CHECK:
            check_call(lat, lng, response_code)


class TestNearestStation(ApiTestCase):
    def test_valid(self):
        station1 = create_station(
            settlement=create_settlement(Settlement.MOSCOW_ID),
            majority=StationMajority.MAIN_IN_CITY_ID,
            station_type=StationType.STATION_ID,
            t_type='train',
        )
        station2 = create_station(
            majority=create_station_majority(),
            title=u'AnyTitle',
            type_choices='aeroex,train',
            station_type=StationType.STATION_ID,
            t_type='train',
        )

        expected_stations = [
            {
                'code': 's{}'.format(station1.id),
                'distance': 0.0,
                'lat': station1.latitude,
                'lng': station1.longitude,
                'majority': station1.majority_id,
                'popular_title': None,
                'short_title': None,
                'station_type': 'station',
                'station_type_name': u'станция',
                'title': station1.title,
                'transport_type': 'train',
                'type': 'station',
                'type_choices': {},
            },
            {
                'code': 's{}'.format(station2.id),
                'distance': 0.0,
                'lat': station2.latitude,
                'lng': station2.longitude,
                'majority': station2.majority_id,
                'popular_title': None,
                'short_title': None,
                'station_type': 'station',
                'station_type_name': u'станция',
                'title': station2.title,
                'transport_type': 'train',
                'type': 'station',
                'type_choices': {
                    'train': {
                        'desktop_url': 'https://rasp.yandex.ru/station/{}/train'.format(station2.id),
                        'touch_url': 'https://t.rasp.yandex.ru/station/{}/train'.format(station2.id),
                    },
                    'aeroex': {
                        'desktop_url': 'https://rasp.yandex.ru/station/{}/aeroex'.format(station2.id),
                        'touch_url': 'https://t.rasp.yandex.ru/station/{}/aeroex'.format(station2.id),
                    },
                }
            }
        ]

        query = {
            'lat': station1.latitude,
            'lng': station1.longitude,
        }
        result = self.api_get_json('nearest_stations', query)
        assert result['stations'] == expected_stations

    def test_coords_check(self):
        def check_call(lat, lng, status_code):
            query = {'lat': str(lat), 'lng': str(lng)}
            assert self.api_get('nearest_stations', query).status_code == status_code

        for lat, lng, response_code in COORDS_TO_CHECK:
            check_call(lat, lng, response_code)
