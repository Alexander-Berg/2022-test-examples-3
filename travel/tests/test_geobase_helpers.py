# -*- coding: utf-8 -*-

import mock

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase

from travel.rasp.api_public.api_public.geobase_helpers import calc_points_distance_km


class TestGeoBaseHelpers(TestCase):
    def test_calc_points_distance_km(self):
        lat_1, lng_1 = 0, 1
        lat_2, lng_2 = 2, 3
        point_1 = create_settlement(title='set_1', latitude=lat_1, longitude=lng_1)
        point_2 = create_settlement(title='set_2', latitude=lat_2, longitude=lng_2)

        with mock.patch('travel.rasp.api_public.api_public.geobase_helpers.geobase.calculate_points_distance') as m_calc_dist:
            m_calc_dist.return_value = 100.
            result = calc_points_distance_km(point_1, point_2)
            m_calc_dist.assert_called_once_with(point_1.latitude, point_1.longitude,
                                                point_2.latitude, point_2.longitude)
            assert result == m_calc_dist.return_value / 1000
