# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from urllib import urlencode

import mock
import pytest
from django.test import Client
from hamcrest import has_entry, assert_that, contains

from common.data_api.min_prices.api import MinPriceStorage
from common.data_api.min_prices.factory import create_min_price
from common.tester.factories import create_settlement, create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.mongo import tmp_collection
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.utils.iterrecipes import product_by_key


class SegmentStub(object):
    def __init__(self, number, start_station, end_station, is_suburban=False):
        self.number = number
        self.start_station = start_station
        self.end_station = end_station
        self.is_suburban = is_suburban


@replace_now('2016-04-20')
@replace_dynamic_setting('TRAIN_PURCHASE_SOLD_OUT_DEFAULT_SALES_DEPTH', 66)
@pytest.mark.dbuser
@pytest.mark.parametrize("request_params,expected_tariffs", [
    # фильтруем по pointFrom и pointTo
    (
        {'pointFrom': 'c2', 'pointTo': 'c213', 'national_version': 'ru'},
        [
            {
                'key': '2-213-train-economy',
                'classes': {u'economy': {u'price': {u'currency': u'RUB', u'value': 1000}}}
            },
            {
                'key': '2-213-train-like_a_boss',
                'classes': {u'like_a_boss': {u'price': {u'currency': u'RUB', u'value': 1000}}}
            }
        ]
    ),
    # с глубиной продаж
    (
        {'pointFrom': 'c2', 'pointTo': 'c213', 'national_version': 'ru', 'withSalesDepth': 'True'},
        [
            {
                'key': '213ФЯ',
                'salesDepth': 66,
                'classes': {}
            },
            {
                'key': '2-213-train-economy',
                'salesDepth': 66,
                'classes': {u'economy': {u'price': {u'currency': u'RUB', u'value': 1000}}}
            },
            {
                'key': '2-213-train-like_a_boss',
                'classes': {u'like_a_boss': {u'price': {u'currency': u'RUB', u'value': 1000}}}
            }
        ]
    ),
])
def test_min_tariffs_short(request_params, expected_tariffs):
    with tmp_collection('min_prices_test') as col:
        storage = MinPriceStorage(col)
        with mock.patch('travel.rasp.train_api.tariffs.train.views.min_price_storage', storage):
            with mock.patch('travel.rasp.train_api.tariffs.train.views.fill_start_and_end_stations_from_thread', autospec=True) as m_segments:
                m_segments.return_value = _create_test_segments()
                _populate_min_prices(col)
                response = Client().get('{}?{}'.format('/ru/api/segments/min-tariffs-short/', urlencode(request_params, doseq=True)))
                assert response.status_code == 200
                assert_that(json.loads(response.content), has_entry('tariffs', contains(*expected_tariffs)))


def _create_test_segments():
    station_a = create_station(settlement=create_settlement())
    station_b = create_station(settlement=create_settlement())
    return [
        SegmentStub('2-213-train-economy', station_a, station_b),
        SegmentStub('213ФЯ', station_a, station_b)
    ]


def _populate_min_prices(collection):
    for id_ in [1, 2, 3]:
        create_settlement(id=id_)

    test_data = product_by_key({
        'object_from_id': [1, 2],
        'object_to_id': [213, 3],
        'date_forward': ['2016-09-10'],
        'type': ['train', 'bus', 'plane'],
        'class': ['economy', 'like_a_boss'],
        'price': [1000, 2000, 3000]
    })
    for item in test_data:
        item['route_uid'] = _format_key(item)
        create_min_price(collection, item)


def _format_key(item):
    return '{object_from_id}-{object_to_id}-{type}-{class}'.format(**item)
