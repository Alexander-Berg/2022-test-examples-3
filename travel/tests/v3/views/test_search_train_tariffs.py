# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
from hamcrest import assert_that, has_entries

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting

from travel.rasp.export.tests.v3.factories import create_station
from travel.rasp.export.tests.v3.helpers import api_get_json, MOCK_TRAIN_RESPONSE


pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


class TestSearchTrainTariffs(object):
    @replace_now('2018-09-10')
    def test_search_train_tariffs(self):
        create_station(id=666), create_station(id=667)

        params = {
            'point_from': 's666',
            'point_to': 's667',
            'date': '2018-09-10',
            'segments_keys': [
                [
                    'train 119A 20180910_10',
                    'train 119A 20180910_12',
                    'train 120A 20180910_10',
                    'train 120A 20180910_12'
                ],
                [
                    'train 222Я 20180910_15',
                    'train 221Я 20180910_15',
                    'train 222Я 20180910_14',
                    'train 221Я 20180910_14'
                ]
            ]
        }

        with mock.patch('travel.rasp.export.export.v3.selling.train_api.get_train_data', return_value=MOCK_TRAIN_RESPONSE), \
                replace_setting('TRAIN_ORDER_DOMAIN', 'touch.train.ru'):
            response = api_get_json('/v3/suburban/search_train_tariffs/', json.dumps(params),
                                    content_type='application/json', method='post')

        assert_that(response, has_entries({
            'train_tariffs_polling': False,
            'train 119A 20180910_12': {
                'selling_info': {
                    'type': 'train',
                    'tariffs': [
                        {
                            'seats': 40,
                            'class_name': 'СВ',
                            'currency': 'RUB',
                            'order_url': 'touch.train.ru/order/?coachType=suite&utm_source=suburbans',
                            'class': 'suite',
                            'value': 10000
                        },
                        {
                            'seats': 150,
                            'class_name': 'сидячие',
                            'currency': 'RUB',
                            'order_url': 'touch.train.ru/order/?coachType=sitting&utm_source=suburbans',
                            'class': 'sitting',
                            'value': 59.31
                        }
                    ]
                }
            },
            'train 222Я 20180910_14': {
                'selling_info':  {
                    'type': 'train',
                    'tariffs': [{
                        'seats': 10,
                        'class_name': 'сидячие',
                        'currency': 'RUB',
                        'order_url': 'touch.train.ru/order/?coachType=sitting&utm_source=suburbans',
                        'class': 'sitting',
                        'value': 1500
                    }]
                }
            }
        }))

        train_data = {
            'segments': [],
            'querying': True
        }
        with mock.patch('travel.rasp.export.export.v3.selling.train_api.get_train_data', return_value=train_data):
            response = api_get_json('/v3/suburban/search_train_tariffs/', json.dumps(params),
                                    content_type='application/json', method='post')
        assert response == {'train_tariffs_polling': True}
