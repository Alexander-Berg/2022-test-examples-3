# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
from django.test import Client

from common.data_api.search_stats.search_stats import SearchStats
from common.models.geo import Settlement, Country
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station, create_thread
from common.tester.testcase import TestCase
from common.utils.text import NBSP

create_thread = create_thread.mutate(__={'calculate_noderoute': True}, t_type=TransportType.TRAIN_ID)
create_station = create_station.mutate(country=Country.RUSSIA_ID, type_choices='train')
create_settlement = create_settlement.mutate(country=Country.RUSSIA_ID)


@mock.patch.object(SearchStats, 'get_top_to')
@mock.patch.object(SearchStats, 'get_top_from')
class TestPopularDirections(TestCase):
    def test_popular_directions(self, m_get_top_from, m_get_top_to):
        client = Client()
        settlement_id = 54
        eburg = create_settlement(
            id=settlement_id, slug='ekb_city_slug',
            title='Екатеринбург', title_ru_genitive='Екатеринбурга', title_ru_accusative='Екатеринбург'
        )
        kiev = create_settlement(
            id=Settlement.KIEV_ID, slug='kiev_city_slug',
            title='Киев', title_ru_genitive='Киева'
        )
        piter = create_settlement(
            id=Settlement.SPB_ID, slug='spb_city_slug',
            title='Санкт-Петербург', title_ru_accusative='Санкт-Петербург',
        )
        eburg_station = create_station(id=101, slug='eburg_station', settlement=eburg)
        kiev_station = create_station(id=201, slug='kiev_station', settlement=kiev)
        piter_station = create_station(id=301, slug='piter_station', settlement=piter)
        moscow_station_1 = create_station(id=401, settlement=Settlement.MOSCOW_ID)
        moscow_station_2 = create_station(id=402, settlement=Settlement.MOSCOW_ID)

        create_thread(schedule_v1=[
            [None, 0, kiev_station],
            [50, 60, moscow_station_1], [70, 80, moscow_station_2],
            [150, 160, piter_station],
            [200, None, eburg_station]
        ])
        create_thread(schedule_v1=[
            [None, 0, eburg_station],
            [50, 60, piter_station],
            [100, 110, moscow_station_2], [120, 130, moscow_station_1],
            [200, None, kiev_station]
        ])

        m_get_top_from.return_value = [
            ('c213', 1000),
            ('c2', 500)
        ]

        m_get_top_to.return_value = [
            ('c143', 1400),
            ('c213', 1200)
        ]

        response = client.get('/ru/settlement/{}/popular-directions/'.format(settlement_id))
        assert response.status_code == 200
        data = json.loads(response.content)

        assert data == {
            'from': {
                'title': 'из' + NBSP + 'Екатеринбурга',
                'points': [
                    {
                        'id': 213,
                        'key': 'c213',
                        'title': 'Москва',
                        'slug': 'Moscow',
                        'directionTitle': 'в' + NBSP + 'Москву',
                        'innerSlug': 'eburg_station',
                        'transportType': 'train'
                    },
                    {
                        'id': 301,
                        'key': 's301',
                        'title': 'Санкт-Петербург',
                        'slug': 'piter_station',
                        'directionTitle': 'в' + NBSP + 'Санкт-Петербург',
                        'innerSlug': 'eburg_station',
                        'transportType': 'train'
                    }
                ]
            },
            'to': {
                'title': 'в' + NBSP + 'Екатеринбург',
                'points': [
                    {
                        'id': 201,
                        'key': 's201',
                        'title': 'Киев',
                        'slug': 'kiev_station',
                        'directionTitle': 'из' + NBSP + 'Киева',
                        'innerSlug': 'eburg_station',
                        'transportType': 'train'
                    },
                    {
                        'id': 213,
                        'key': 'c213',
                        'title': 'Москва',
                        'slug': 'Moscow',
                        'directionTitle': 'из' + NBSP + 'Москвы',
                        'innerSlug': 'eburg_station',
                        'transportType': 'train'
                    }
                ]
            }
        }

        m_get_top_from.assert_called_with('c54', 'all', 'c', limit=8)
        m_get_top_to.assert_called_with('c54', 'all', 'c', limit=8)


@mock.patch.object(SearchStats, 'get_top_to')
@mock.patch.object(SearchStats, 'get_top_from')
class TestTransportPopularDirections(TestCase):
    def test_transport_popular_directions(self, m_get_top_from, m_get_top_to):
        client = Client()
        settlement_id = 54
        create_settlement(id=settlement_id, title='Екатеринбург', slug='ekb_city_slug')
        create_settlement(id=Settlement.KIEV_ID, title='Киев', slug='kiev_city_slug')
        create_settlement(id=Settlement.SPB_ID, title='Санкт-Петербург', slug='spb_city_slug')

        m_get_top_from.return_value = [
            ('c213', 1000),
            ('c2', 500),
        ]

        m_get_top_to.return_value = [
            ('c143', 1400),
            ('c213', 1200),
        ]

        response = client.get('/ru/settlement/ekb_city_slug/transport-popular-directions/?t_type=plane&limit=5')
        assert response.status_code == 200
        data = json.loads(response.content)

        assert data == {
            'fromSettlement': [
                {
                    'to':
                        {
                            'slug': 'Moscow',
                            'key': 'c213',
                            'title': 'Москва',
                        },
                    'from':
                        {
                            'slug': 'ekb_city_slug',
                            'key': 'c54',
                            'title': 'Екатеринбург',
                        },
                },
                {
                    'to':
                        {
                            'slug': 'spb_city_slug',
                            'key': 'c2',
                            'title': 'Санкт-Петербург',
                        },
                    'from':
                        {
                            'slug': 'ekb_city_slug',
                            'key': 'c54',
                            'title': 'Екатеринбург',
                        },
                },
            ],
            'toSettlement': [
                {
                    'to':
                        {
                            'slug': 'ekb_city_slug',
                            'key': 'c54',
                            'title': 'Екатеринбург',
                        },
                    'from':
                        {
                            'slug': 'kiev_city_slug',
                            'key': 'c143',
                            'title': 'Киев',
                        },
                },
                {
                    'to':
                        {
                            'slug': 'ekb_city_slug',
                            'key': 'c54',
                            'title': 'Екатеринбург',
                        },
                    'from':
                        {
                            'slug': 'Moscow',
                            'key': 'c213',
                            'title': 'Москва',
                        },
                },
            ],
        }

        m_get_top_from.assert_called_with('c54', 'plane', 'c', limit=8)
        m_get_top_to.assert_called_with('c54', 'plane', 'c', limit=8)
