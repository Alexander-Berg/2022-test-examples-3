# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import hamcrest
import mock
import pytest
import six
from django.test import Client

from common.data_api.min_prices.api import min_price_storage
from common.data_api.search_stats.search_stats import search_stats
from common.models.currency import Price
from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_settlement, create_station
from common.utils.geobase import geobase


@pytest.mark.dbuser
@mock.patch.object(search_stats, 'get_top_to', autospec=True)
@mock.patch.object(min_price_storage, 'find_best_offers', autospec=True)
@mock.patch.object(geobase, 'linguistics', side_effect=RuntimeError)
def test_train_popular_directions(m_linguistics, m_find_best_offers, m_get_top_to):
    departure_city = create_settlement(
        title='Город оправления',
        title_ru_genitive='Города оправления',
        country_id=Country.RUSSIA_ID,
        slug='departure_city',
        _geo_id=1
    )
    arrival_city = create_settlement(
        title='Город прибытия',
        title_ru_accusative='Город прибытия',
        slug='arrival_city'
    )
    m_get_top_to.return_value = [(arrival_city.point_key, mock.sentinel.total)]
    m_find_best_offers.return_value = {
        arrival_city: {'departure_date': date(2000, 1, 1), 'price': Price(1000), 'number': '123Х'}
    }
    create_thread(
        t_type='train',
        schedule_v1=[
            [None, 0, create_station(settlement=departure_city)],
            [10, None, create_station(settlement=arrival_city)]
        ],
        __={'calculate_noderoute': True}
    )
    response = Client().get('/ru/api/train-popular-directions/?geoId=1')

    assert response.status_code == 200
    assert response.data == {
        'departureCity': {
            'title': departure_city.title,
            'directionTitle': 'из\N{no-break space}{}'.format(departure_city.title_ru_genitive),
            'id': departure_city.id,
            'key': departure_city.point_key,
            'slug': 'departure_city'
        },
        'arrivalCities': [
            {
                'title': arrival_city.title,
                'directionTitle': 'в\N{no-break space}{}'.format(arrival_city.title_ru_accusative),
                'id': arrival_city.id,
                'imageUrl': hamcrest.match_equality(hamcrest.instance_of(six.text_type)),
                'key': arrival_city.point_key,
                'slug': 'arrival_city',
                'bestOffer': {
                    'departureDate': '2000-01-01',
                    'price': {'value': 1000, 'currency': 'RUR'},
                    'number': '123Х'
                }
            }
        ]
    }
    m_get_top_to.assert_called_once_with(departure_city.point_key, 'train', search_type='c', limit=20)
    m_find_best_offers.assert_called_once_with(
        t_type=TransportType.objects.get(id=TransportType.TRAIN_ID),
        departure_settlement=departure_city,
        arrival_settlements=[arrival_city]
    )
    m_linguistics.assert_called_once_with(departure_city._geo_id, 'ru')

    response = Client().get('/ru/api/train-popular-directions/?geoId=999')

    assert response.status_code == 404
    assert response.data == {'errors': {'geoId': 'Invalid value. Settlement was not found.'}}
