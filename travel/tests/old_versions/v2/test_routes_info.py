# -*- coding: utf-8 -*-

from common.models.transport import TransportType

from common.tester.factories import create_thread, create_station

from travel.rasp.api_public.tests.old_versions.v2 import ApiV2TestCase


class TestSearch(ApiV2TestCase):
    create_thread = create_thread.mutate(__={'calculate_noderoute': True})

    def test_routes_info(self):
        sirena_params = {'from': 'sirena_from', 'to': 'sirena_to', 'system': 'sirena'}
        station_from = create_station(__={'codes': {'sirena': 'sirena_from', 'iata': 'iata_from'}})
        station_to = create_station(__={'codes': {'sirena': 'sirena_to', 'iata': 'iata_to'}})

        def add_thread(time, number='one'):
            self.create_thread(
                t_type=TransportType.PLANE_ID,
                number=number,
                schedule_v1=[
                    [None, 0, station_from],
                    [time, None, station_to],
                ])

        result = self.api_get_json('route', sirena_params)
        assert result == {
            'min_duration': 0,
            'routes_count': 0
        }

        add_thread(25)
        for params in [sirena_params,
                       {'from': 'iata_from', 'to': 'iata_to', 'system': 'iata'},
                       {'from': station_from.point_key, 'to': station_to.point_key}]:
            result = self.api_get_json('route', params)
            assert result == {
                'min_duration': 25,
                'routes_count': 1
            }

        add_thread(5)
        result = self.api_get_json('route', sirena_params)
        assert result == {
            'min_duration': 5,
            'routes_count': 1
        }

        add_thread(3, 'another')
        result = self.api_get_json('route', sirena_params)
        assert result == {
            'min_duration': 3,
            'routes_count': 2
        }
