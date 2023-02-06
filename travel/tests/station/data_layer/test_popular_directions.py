# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, contains, has_properties

from common.data_api.search_stats.search_stats import search_stats
from common.models.transport import TransportType
from common.models.geo import Country
from common.tester.factories import create_settlement, create_station, create_thread

from travel.rasp.morda_backend.morda_backend.station.data_layer.popular_directions import get_station_popular_directions
from travel.rasp.morda_backend.morda_backend.station.request_serialization import PopularDirectionsContext


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={'calculate_noderoute': True}, t_type=TransportType.TRAIN_ID)
create_station = create_station.mutate(t_type=TransportType.TRAIN_ID, country=Country.RUSSIA_ID)
create_settlement = create_settlement.mutate(country=Country.RUSSIA_ID)


def test_station_popular_directions_old():
    settlement_0 = create_settlement(id=100, title='City0', slug='city0')
    station_0_1 = create_station(
        id=101, title='Station01', slug='station01', settlement=settlement_0, type_choices='train'
    )

    settlement_1 = create_settlement(id=110, title='City1', slug='city1')
    station_1_1 = create_station(
        id=111, title='Station11', slug='station11', settlement=settlement_1, type_choices='train'
    )
    station_1_2 = create_station(
        id=112, title='Station12', slug='station12', settlement=settlement_1, type_choices='train'
    )

    settlement_2 = create_settlement(id=120, title='City2', slug='city2')
    station_2_1 = create_station(
        id=121, title='Station21', slug='station21', settlement=settlement_2, type_choices='train'
    )

    settlement_3 = create_settlement(id=130, title='City3', slug='city3')
    create_station(id=131, title='Station31', slug='station31', settlement=settlement_3)

    settlement_4 = create_settlement(id=140, title='City4', slug='city4')
    station_4_1 = create_station(
        id=141, title='Station41', slug='station41', settlement=settlement_4, type_choices='train'
    )
    station_4_2 = create_station(
        id=142, title='Station42', slug='station42', settlement=settlement_4, type_choices='train'
    )

    create_station(id=151, title='Station51', slug='station51')

    create_thread(schedule_v1=[
        [None, 0, station_0_1], [10, 15, station_1_1], [20, 25, station_1_2],
        [30, 35, station_2_1], [40, 45, station_4_1], [50, None, station_4_2]
    ])
    create_thread(schedule_v1=[
        [None, 0, station_4_2], [10, 15, station_4_1], [20, 25, station_2_1],
        [30, 35, station_1_2], [40, 45, station_1_1], [50, None, station_0_1]
    ])

    with mock.patch.object(
            search_stats, 'get_top_from',
            return_value=[
                ('c110', 100), ('s111', 99), ('c120', 98), ('s121', 97),
                ('c130', 96), ('c100', 95), ('s151', 94), ('s141', 93), ('s142', 92)
            ]
    ):
        with mock.patch.object(
                search_stats, 'get_top_to',
                return_value=[
                    ('s111', 100), ('c110', 99), ('s121', 98), ('c120', 97),
                    ('s131', 96), ('c100', 95), ('s151', 94), ('s141', 93), ('s142', 92)
                ]
        ):
            context = PopularDirectionsContext(101, 'train', 6)
            result = get_station_popular_directions(context)

            assert 'station' in result
            assert 'from_points' in result
            assert 'to_points' in result

            assert result['station'].point_key == 's101'
            assert result['station'].slug == 'station01'
            assert result['station'].title == 'City0'
            assert result['t_type_code'] == 'train'

            assert_that(result['from_points'], contains(
                has_properties({'point_key': 'c110', 'slug': 'city1', 'title': 'City1'}),
                has_properties({'point_key': 's111', 'slug': 'station11', 'title': 'Station11'}),
                has_properties({'point_key': 's121', 'slug': 'station21', 'title': 'City2'}),
                has_properties({'point_key': 'c130', 'slug': 'city3', 'title': 'City3'}),
                has_properties({'point_key': 's151', 'slug': 'station51', 'title': 'Station51'}),
                has_properties({'point_key': 's141', 'slug': 'station41', 'title': 'Station41'}),
            ))

            assert_that(result['to_points'], contains(
                has_properties({'point_key': 's111', 'slug': 'station11', 'title': 'Station11'}),
                has_properties({'point_key': 'c110', 'slug': 'city1', 'title': 'City1'}),
                has_properties({'point_key': 's121', 'slug': 'station21', 'title': 'Station21'}),
                has_properties({'point_key': 's131', 'slug': 'station31', 'title': 'Station31'}),
                has_properties({'point_key': 's151', 'slug': 'station51', 'title': 'Station51'}),
                has_properties({'point_key': 's141', 'slug': 'station41', 'title': 'Station41'}),
            ))

            context = PopularDirectionsContext(151, 'train')
            result = get_station_popular_directions(context)

            assert 'station' in result
            assert result['station'].point_key == 's151'
            assert result['station'].slug == 'station51'
            assert result['station'].title == 'Station51'
