# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.geo import Station, Settlement
from common.tester.factories import create_station, create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.search_sample_point import (
    search_sample_point_json
)


class TestSearchSamplePointJson(TestCase):
    def test_search_sample_station_json(self):
        station_id = 777
        station = create_station(id=station_id)

        with mock.patch.object(Station, 'L_popular_title', return_value='Popular title') as m_L_popular_title:
            assert search_sample_point_json(station, 'uk') == {
                'key': 's777',
                'title': 'Popular title'
            }

        m_L_popular_title.assert_called_once_with(lang='uk')

    def test_search_sample_settlement_json(self):
        settlement_id = 888
        settlement = create_settlement(id=settlement_id)

        with mock.patch.object(Settlement, 'L_popular_title', return_value='Popular title') as m_L_popular_title:
            assert search_sample_point_json(settlement, 'uk') == {
                'key': 'c888',
                'title': 'Popular title'
            }

        m_L_popular_title.assert_called_once_with(lang='uk')
