# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from django.test import Client

from common.data_api.search_stats.search_stats import search_stats
from common.models.geo import Country
from common.tester.factories import create_thread, create_settlement, create_station
from travel.rasp.morda_backend.morda_backend.settlement.data_layer.train_popular_directions import (
    KALININGRAD_REGION_ID, KRYM_REGION_ID
)


@pytest.mark.dbuser
@mock.patch.object(search_stats, 'get_top_to', autospec=True)
def test_train_popular_directions(m_get_top_to):
    departure_city = create_settlement(title='Город оправления', title_ru_genitive='Города оправления',
                                       country_id=Country.RUSSIA_ID, slug='departure_city')
    city_1 = create_settlement(title='Город 1', title_ru_accusative='Город 1', slug='gorod-1')
    city_2 = create_settlement(title='Город 2', title_ru_accusative='Город 2', slug='gorod-2')
    city_3 = create_settlement(title='Город 3', title_ru_accusative='Город 3', slug='gorod-3')
    city_4 = create_settlement(title='Город 4', title_ru_accusative='Город 4', slug='gorod-4')
    city_5 = create_settlement(title='Город 5', title_ru_accusative='Город 5', slug='gorod-5')
    city_in_krym = create_settlement(region=dict(id=KRYM_REGION_ID))
    city_in_kaliningrad = create_settlement(region=dict(id=KALININGRAD_REGION_ID))
    city_without_threads = create_settlement()

    m_get_top_to.return_value = (
        (city.point_key, mock.sentinel.total)
        for city in (city_in_krym, city_in_kaliningrad, city_without_threads, city_1, city_2, city_3, city_4, city_5)
    )

    for city in (city_in_krym, city_in_kaliningrad, city_1, city_2, city_3, city_4, city_5):
        create_thread(t_type='train',
                      schedule_v1=[[None, 0, create_station(settlement=departure_city)],
                                   [10, None, create_station(settlement=city)]],
                      __={'calculate_noderoute': True})

    response = Client().get('/ru/settlement/{}/train-popular-directions/'.format(departure_city.id))

    assert response.status_code == 200
    assert response.data == {
        'departureCity': {
            'title': departure_city.title,
            'directionTitle': 'из\N{no-break space}Города оправления',
            'id': departure_city.id,
            'key': departure_city.point_key,
            'slug': 'departure_city'
        },
        'arrivalCities': [
            {
                'title': city_1.title,
                'directionTitle': 'в\N{no-break space}Город 1',
                'id': city_1.id,
                'key': city_1.point_key,
                'slug': 'gorod-1'
            },
            {
                'title': city_2.title,
                'directionTitle': 'в\N{no-break space}Город 2',
                'id': city_2.id,
                'key': city_2.point_key,
                'slug': 'gorod-2'
            },
            {
                'title': city_3.title,
                'directionTitle': 'в\N{no-break space}Город 3',
                'id': city_3.id,
                'key': city_3.point_key,
                'slug': 'gorod-3'
            },
            {
                'title': city_4.title,
                'directionTitle': 'в\N{no-break space}Город 4',
                'id': city_4.id,
                'key': city_4.point_key,
                'slug': 'gorod-4'
            }
        ]
    }
    m_get_top_to.assert_called_once_with(departure_city.point_key, 'train', search_type='c', limit=20)
