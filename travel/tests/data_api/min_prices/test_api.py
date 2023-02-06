# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import hamcrest
import pytest
import six

from common.data_api.min_prices.api import MinPriceStorage
from common.data_api.min_prices.factory import create_min_price, create_min_price_dict
from common.models.currency import Price
from common.models.geo import Settlement
from common.models.transport import TransportType
from common.models_utils.geo import Point
from common.tester.factories import create_station, create_settlement
from common.tester.utils.datetime import replace_now
from common.tester.utils.mongo import tmp_collection


@replace_now('2016-09-09')
@pytest.mark.dbuser
@pytest.mark.parametrize("point_from_key,point_to_key,t_codes,date_from,expected", [
    (
        'c2', 'c213', None, None,
        [
            {'route_uid': '2-213-bus-economy', 'price': 1000},
            {'route_uid': '2-213-train-economy', 'price': 2000},
            {'route_uid': '2-213-train-compartment', 'price': 2100},
            {'route_uid': '2-213-plane-economy', 'price': 3000}
        ]
    ),
    (
        's20', 's2130', None, None,
        [
            {'route_uid': '20-2130-train-economy', 'price': 1200}
        ]
    ),
    (
        'c2', 'c213', ['bus', 'train'], None,
        [
            {'route_uid': '2-213-bus-economy', 'price': 1000},
            {'route_uid': '2-213-train-economy', 'price': 2000},
            {'route_uid': '2-213-train-compartment', 'price': 2100}
        ]
    ),
    (
        'c2', 'c213', None, date(2016, 10, 10),
        [
            {'route_uid': '2-213-train-economy', 'price': 2500}
        ]
    ),
])
def test_get_min_prices(point_from_key, point_to_key, t_codes, date_from, expected):
    create_station(id=20)
    create_station(id=2130)
    create_settlement(id=2)
    point_from = Point.get_by_key(point_from_key)
    point_to = Point.get_by_key(point_to_key)
    with tmp_collection('min_prices_test') as col:
        storage = MinPriceStorage(col)
        _create_min_prices(storage)
        result = storage.get_min_prices(point_from, point_to, t_codes, date_from)
        hamcrest.assert_that(
            result,
            hamcrest.contains_inanyorder(
                *(hamcrest.has_entries(item) for item in expected)
            )
        )


@replace_now('2000-01-01')
@pytest.mark.dbuser
def test_find_best_offers():
    spb = Settlement(id=2)
    ekb = Settlement(id=54)
    msk = Settlement(id=213)
    kiev = Settlement(id=143)

    with tmp_collection('min_prices_test') as col:
        create_min_price(col, {'object_to_id': ekb.id, 'date_forward': '1999-12-31', 'price': 1000})
        create_min_price(col, {'object_to_id': ekb.id, 'date_forward': '2000-01-01', 'price': 1500, 'type': 'bus'})
        create_min_price(col, {'object_to_id': ekb.id, 'date_forward': '2000-01-01', 'price': 2000})
        create_min_price(col, {'object_to_id': ekb.id, 'date_forward': '2000-01-02', 'price': 3000})
        create_min_price(col, {'object_to_id': msk.id, 'date_forward': '1999-12-31', 'price': 500})
        create_min_price(col, {'object_to_id': msk.id, 'date_forward': '2000-01-01', 'price': 600, 'type': 'bus'})
        create_min_price(col, {'object_to_id': msk.id, 'date_forward': '2000-01-02', 'price': 1000})
        create_min_price(col, {'object_to_id': msk.id, 'date_forward': '2000-01-03', 'price': 2000})

        assert MinPriceStorage(col).find_best_offers(
            t_type=TransportType.objects.get(id=TransportType.TRAIN_ID),
            departure_settlement=spb,
            arrival_settlements=[ekb, kiev, msk]
        ) == {
            ekb: {
                'departure_date': date(2000, 1, 1),
                'number': hamcrest.match_equality(hamcrest.instance_of(six.text_type)),
                'price': Price(2000),
            },
            msk: {
                'departure_date': date(2000, 1, 2),
                'number': hamcrest.match_equality(hamcrest.instance_of(six.text_type)),
                'price': Price(1000),
            }
        }


def _create_min_prices(storage):
    data = [
        {
            'object_from_id': 2,
            'object_from_type': 'Settlement',
            'object_to_id': 213,
            'object_to_type': 'Settlement',
            'type': 'train',
            'date_forward': '2016-09-10',
            'price': 10000
        },
        {
            'object_from_id': 2,
            'object_from_type': 'Settlement',
            'object_to_id': 213,
            'object_to_type': 'Settlement',
            'type': 'plane',
            'date_forward': '2016-09-10',
            'price': 3000
        },
        {
            'object_from_id': 2,
            'object_from_type': 'Settlement',
            'object_to_id': 213,
            'object_to_type': 'Settlement',
            'type': 'train',
            'date_forward': '2016-10-11',
            'price': 2500
        },
        {
            'object_from_id': 2,
            'object_from_type': 'Settlement',
            'object_to_id': 213,
            'object_to_type': 'Settlement',
            'type': 'train',
            'class': 'compartment',
            'date_forward': '2016-09-10',
            'price': 2100
        },
        {
            'object_from_id': 2,
            'object_from_type': 'Settlement',
            'object_to_id': 213,
            'object_to_type': 'Settlement',
            'type': 'train',
            'date_forward': '2016-09-10',
            'price': 2000
        },
        {
            'object_from_id': 20,
            'object_from_type': 'Station',
            'object_to_id': 2130,
            'object_to_type': 'Station',
            'type': 'train',
            'date_forward': '2016-09-10',
            'price': 1200
        },
        {
            'object_from_id': 2,
            'object_from_type': 'Settlement',
            'object_to_id': 213,
            'object_to_type': 'Settlement',
            'type': 'bus',
            'date_forward': '2016-09-10',
            'price': 1000
        },
        {
            'object_from_id': 2,
            'object_from_type': 'Station',
            'object_to_id': 213,
            'object_to_type': 'Station',
            'type': 'train',
            'date_forward': '2016-09-11',
            'timestamp': '2016-09-01 00:33:44',
            'price': 900
        }
    ]
    min_prices = []
    for item in data:
        min_price_dict = create_min_price_dict(item)
        route_uid = '{from_id}-{to_id}-{type}-{class_}'.format(
            from_id=item['object_from_id'],
            to_id=item['object_to_id'],
            type=item['type'],
            class_=item.get('class', 'economy'),
        )
        min_price_dict['route_uid'] = route_uid
        min_prices.append(min_price_dict)
    storage.save_many(min_prices)
    storage.remove_old_rows()
    storage.create_indexes()
