# coding: utf8

import pytest
from hamcrest import has_entries, assert_that, contains_inanyorder


from travel.rasp.tasks.min_prices.yt_to_mongo import prepare_data


@pytest.mark.dbuser
def test_prepare_data():
    """
    Мы должны корректировать классы минимальных цен:
      'economy-bus' -> 'economy'
      'None' -> 'platzkart'
      'first' -> пропускаем
    """
    prices = [
        {'class': 'economy-bus', 'route_uid': '1'},
        {'class': 'economy', 'route_uid': '2'},
        {'class': 'None', 'route_uid': '3'},
        {'class': 'first', 'route_uid': '4'},
        {'class': 'business', 'route_uid': '5'}
    ]
    for price in prices:
        price['object_from_id'] = '2'
        price['object_from_type'] = 'Settlement'
        price['object_to_id'] = '2006004'
        price['object_to_type'] = 'Station'
    prepared = prepare_data(prices)
    assert_that(
        prepared,
        contains_inanyorder(
            has_entries({'class': 'economy', 'route_uid': '1'}),
            has_entries({'class': 'economy', 'route_uid': '2'}),
            has_entries({'class': 'platzkart', 'route_uid': '3'})
        )
    )


@pytest.mark.dbuser
def test_prepare_data_tuple():
    price = {'class': 'plane',
             'route_uid': '1',
             'object_from_id': '2',
             'object_from_type': 'SettlementTuple',
             'object_to_id': '2',
             'object_to_type': 'StationTuple'
             }
    prepared = prepare_data([price])
    assert_that(
        prepared,
        contains_inanyorder(
            has_entries({'class': 'plane', 'object_from_type': 'Settlement', 'object_to_type': 'Station'})
        )
    )
