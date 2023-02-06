# -*- coding: utf-8 -*-
from travel.rasp.export.export.views.airports import get_airports
from common.models.geo import CodeSystem

from common.tester.testcase import TestCase
from common.tester.factories import create_station, create_station_code


class TestAirports(TestCase):
    def test_valid(self):

        def create_station_with_systems(system_codes, **station_kwargs):
            station_kwargs.setdefault('t_type', 'plane')
            station = create_station(**station_kwargs)

            for system_code, station_code in system_codes:
                system = CodeSystem.objects.get(code=system_code)
                create_station_code(station=station, system=system, code=str(station_code))

            return station

        create_station_with_systems(
            [('iata', 1), ['icao', 10]], title=u'st5', title_ru=u'st5_ру', title_en=u'st5_en',
            settlement={'title': 'sett_st5', 'title_ru': u'sett_st5_ру', 'title_en': u'sett_st5_en', '_geo_id': 5},
        )
        create_station_with_systems(
            [['icao', 2]], title=u'st4', title_ru=u'st4_ру', title_en=u'st4_en',
            settlement={'title': 'sett_st4', 'title_ru': u'sett_st4_ру', 'title_en': u'sett_st4_en', '_geo_id': 4},
        )
        create_station_with_systems(
            [['iata', 3]], title=u'st2', title_ru=u'st2_ру', title_en=u'st2_en',
            settlement={'title': 'sett_st2', 'title_ru': u'sett_st2_ру', 'title_en': u'sett_st2_en', '_geo_id': 2},
        )
        create_station_with_systems(
            [('mosgortrans', 4), ['kladr', 4]], title=u'st3', title_ru=u'st3_ру', title_en=u'st3_en',
            settlement={'title': 'sett_st3', 'title_ru': u'sett_st3_ру', 'title_en': u'sett_st3_en', '_geo_id': 3},
        )
        create_station_with_systems(
            [], title=u'st1', title_ru=u'st1_ру', title_en=u'st1_en',
            settlement={'title': 'sett_st1', 'title_ru': u'sett_st1_ру', 'title_en': u'sett_st1_en', '_geo_id': 1},
        )

        # bad t_type stations
        create_station_with_systems([('iata', 8), ['icao', 8]], t_type='bus')
        create_station_with_systems([['icao', 9]], t_type='bus')

        result = get_airports()

        expected = [
            {
                'title': u'st1', 'title_ru': u'st1_ру', 'title_en': u'st1_en',
                'time_zone': u'Europe/Moscow',
                'city': {
                    'title': u'sett_st1', 'title_ru': u'sett_st1_ру', 'title_en': u'sett_st1_en',
                    'geo_id': 1,
                }
            },
            {
                'title': u'st2', 'title_ru': u'st2_ру', 'title_en': u'st2_en',
                'time_zone': u'Europe/Moscow',
                'iata': u'3',
                'city': {
                    'title': u'sett_st2', 'title_ru': u'sett_st2_ру', 'title_en': u'sett_st2_en',
                    'geo_id': 2,
                }
            },
            {
                'title': u'st3', 'title_ru': u'st3_ру', 'title_en': u'st3_en',
                'time_zone': u'Europe/Moscow',
                'city': {
                    'title': u'sett_st3', 'title_ru': u'sett_st3_ру', 'title_en': u'sett_st3_en',
                    'geo_id': 3,
                }
            },
            {
                'title': u'st4', 'title_ru': u'st4_ру', 'title_en': u'st4_en',
                'time_zone': u'Europe/Moscow',
                'icao': u'2',
                'city': {
                    'title': u'sett_st4', 'title_ru': u'sett_st4_ру', 'title_en': u'sett_st4_en',
                    'geo_id': 4,
                }
            },
            {
                'title': u'st5', 'title_ru': u'st5_ру', 'title_en': u'st5_en',
                'time_zone': u'Europe/Moscow',
                'iata': u'1',
                'icao': u'10',
                'city': {
                    'title': u'sett_st5', 'title_ru': u'sett_st5_ру', 'title_en': u'sett_st5_en',
                    'geo_id': 5,
                }
            },
        ]

        assert result == expected
