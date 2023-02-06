# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
import mock

import httpretty
import pytest
from django.conf import settings
from django.test.client import Client
from django.utils.http import urlencode

from common.tester.utils.replace_setting import replace_setting

from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_saver import tariff_direction_info_saver


@pytest.fixture(autouse=True)
def do_before_tests():
    with replace_setting('SEARCH_API_URL', 'http://search/api/'):   # request pool cleanup run before test
        with replace_setting('ENABLE_PROXY_INDEXER_TO_SEARCH_API', True):  # for debug
            with replace_setting('SEARCH_API_INDEXER_TIMEOUT', 999.9):  # for debug
                yield


@httpretty.activate
def test_data_index_request():
    httpretty.register_uri(
        httpretty.POST, '{}{}'.format(settings.SEARCH_API_URL, 'indexer/api/direction/'), status=200, body=''
    )

    test_json_data = [{'coaches': 'my_coaches', 'electronic_ticket': True}]

    with mock.patch.object(tariff_direction_info_saver, 'save', return_value=None):
        path = '/indexer/api/direction/?' + urlencode({
            'departure_point_express_id': 1,
            'arrival_point_express_id': 2,
            'departure_date': '2019-09-01',
        })

        response = Client().post(path)
        assert response.status_code == 400
        assert response.data == 'Error: incorrect json'

        response = Client().post(path, json.dumps([{}]), content_type='application/json')
        assert response.status_code == 200
        args, _ = tariff_direction_info_saver.save.call_args_list[-1]
        assert args[0]['data'] == [{}]

        response = Client().post(path, json.dumps([]), content_type='application/json')
        assert response.status_code == 200
        args, _ = tariff_direction_info_saver.save.call_args_list[-1]
        assert args[0]['data'] == []

        response = Client().post(path, json.dumps(test_json_data), content_type='application/json')
        assert response.status_code == 200
        args, _ = tariff_direction_info_saver.save.call_args_list[-1]
        assert args[0]['data'] == test_json_data

        assert len(httpretty.latest_requests()) == 3
        assert httpretty.last_request().querystring['departure_point_express_id'] == ['1']
        assert httpretty.last_request().querystring['arrival_point_express_id'] == ['2']
        assert httpretty.last_request().querystring['departure_date'] == ['2019-09-01']
        assert httpretty.last_request().parsed_body == test_json_data
