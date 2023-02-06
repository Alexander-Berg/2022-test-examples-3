# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from common.data_api.search_stats.search_stats import search_stats
from common.models.geo import Country
from common.tester.factories import create_thread, create_settlement, create_station
from travel.rasp.train_api.data_layer.train_popular_directions import (
    KALININGRAD_REGION_ID, KRYM_REGION_ID, get_train_popular_directions
)


@pytest.mark.dbuser
@mock.patch.object(search_stats, 'get_top_to', autospec=True)
def test_get_train_popular_directions(m_get_top_to):
    departure_city = create_settlement(country_id=Country.RUSSIA_ID)
    city_1 = create_settlement()
    city_2 = create_settlement()
    city_3 = create_settlement()
    city_4 = create_settlement()
    city_5 = create_settlement()
    city_in_krym = create_settlement(region=dict(id=KRYM_REGION_ID))
    city_in_kaliningrad = create_settlement(region=dict(id=KALININGRAD_REGION_ID))
    city_without_threads = create_settlement()

    m_get_top_to.return_value = (
        (city.point_key, mock.sentinel.total)
        for city in (city_in_krym, city_in_kaliningrad, city_without_threads, city_1, city_2, city_3, city_4, city_5)
    )

    for city in (city_in_krym, city_in_kaliningrad, city_1, city_2, city_3, city_4, city_5):
        create_thread(
            t_type='train',
            schedule_v1=[
                [None, 0, create_station(settlement=departure_city)],
                [10, None, create_station(settlement=city)]
            ],
            __={'calculate_noderoute': True}
        )

    assert get_train_popular_directions(departure_city) == {
        'departure_city': departure_city,
        'arrival_cities': [city_1, city_2, city_3, city_4]
    }
    m_get_top_to.assert_called_once_with(departure_city.point_key, 'train', search_type='c', limit=20)
