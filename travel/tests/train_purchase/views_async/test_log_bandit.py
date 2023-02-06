# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import base64
import json

import mock
import pytest
from django.test import Client
from google.protobuf.timestamp_pb2 import Timestamp
from hamcrest import assert_that, has_entries

from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_bandit_api.logging import bandit_train_details_logger
from travel.rasp.train_bandit_api.proto import api_pb2

pytestmark = [pytest.mark.dbuser]


@replace_now('2020-04-01')
@mock.patch.object(bandit_train_details_logger, 'info')
@replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_LOGGING', True)
def test_log_bandit(m_bandit_train_details_log):
    create_station(
        __={'codes': {'express': '1000'}}, id=2000001, title='Оттуда',
        settlement={'title': 'Город Оттуда', '_geo_id': 201},
    )
    create_station(
        __={'codes': {'express': '1001'}}, id=2000002, title='Сюда',
        settlement={'title': 'Город Сюда', '_geo_id': 202},
    )

    request_body = {
        'departure': '2020-04-30T10:00:00+03:00',
        'arrival': '2020-04-30T15:00:00+03:00',
        'stationFromId': 2000001,
        'stationToId': 2000002,
        'trainType': 'some train',
        'carType': 'some car',
        'serviceClass': 'some class',
        'price': 150.00,
    }

    headers = {
        'HTTP_X_YA_EXPBOXES': 'some buckets',
        'HTTP_X_REQUEST_ID': 'some reqid',
        'HTTP_X_YA_YANDEXUID': 'some uid',
        'HTTP_X_YA_USER_DEVICE': 'some device',
    }

    response = Client().post(
        '/ru/api/log-bandit/?icookie={}'.format('some_cookie'),
        data=json.dumps(request_body),
        content_type='application/json',
        **headers
    )

    assert response.status_code == 200
    assert_that(
        m_bandit_train_details_log.call_args.kwargs['extra'],
        has_entries({
            'arrival_dt': 1588248000,
            'car_type': 'some car',
            'departure_dt': 1588230000,
            'event_type': 'passenger-details',
            'fee': None,
            'icookie': 'some_cookie',
            'is_bandit_fee_applied': None,
            'point_from': 's2000001',
            'point_to': 's2000002',
            'price': 150.0,
            'req_id': 'some reqid',
            'service_class': 'some class',
            'station_from_geo_id': 201,
            'station_to_geo_id': 202,
            'test_buckets': 'some buckets',
            'timestamp': 1585688400,
            'train_type': 'some train',
            'user_device': 'some device',
            'yandex_uid': 'some uid',
        }),
    )


@replace_now('2020-04-01')
@mock.patch.object(bandit_train_details_logger, 'info')
@replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_LOGGING', True)
def test_log_bandit_with_token(m_bandit_train_details_log):
    create_station(
        __={'codes': {'express': '1000'}}, id=2000001, title='Оттуда',
        settlement={'title': 'Город Оттуда', '_geo_id': 201},
    )
    create_station(
        __={'codes': {'express': '1001'}}, id=2000002, title='Сюда',
        settlement={'title': 'Город Сюда', '_geo_id': 202},
    )
    context = api_pb2.TContext(
        ICookie='icookie123',
        PointFrom='point_from',
        PointTo='point_to',
        Arrival=Timestamp(seconds=1588248001),
        Departure=Timestamp(seconds=1588230001),
        TrainType='train_type',
        CarType='car_type',
    )
    fee_calculation_token = api_pb2.TFeeCalculationToken(
        Context=context,
        Permille=130,
        IsBanditFeeApplied=True,
        RequestedBanditType='fix11',
        ActualBanditType='fix13',
        ActualBanditVersion=444,
    )
    request_body = {
        'departure': '2020-04-30T10:00:00+03:00',
        'arrival': '2020-04-30T15:00:00+03:00',
        'stationFromId': 2000001,
        'stationToId': 2000002,
        'trainType': 'some train',
        'carType': 'some car',
        'serviceClass': 'some class',
        'price': 150.00,
        'feeCalculationToken': base64.b64encode(fee_calculation_token.SerializeToString())
    }

    headers = {
        'HTTP_X_YA_EXPBOXES': 'some buckets',
        'HTTP_X_REQUEST_ID': 'some reqid',
        'HTTP_X_YA_YANDEXUID': 'some uid',
        'HTTP_X_YA_USER_DEVICE': 'some device',
    }

    response = Client().post(
        '/ru/api/log-bandit/?icookie={}'.format('some_cookie'),
        data=json.dumps(request_body),
        content_type='application/json',
        **headers
    )

    assert response.status_code == 200
    assert_that(
        m_bandit_train_details_log.call_args.kwargs['extra'],
        has_entries({
            'arrival_dt': 1588248001,
            'car_type': 'car_type',
            'departure_dt': 1588230001,
            'event_type': 'passenger-details',
            'fee': 0.13,
            'icookie': 'icookie123',
            'is_bandit_fee_applied': True,
            'point_from': 'point_from',
            'point_to': 'point_to',
            'price': 150.0,
            'req_id': 'some reqid',
            'service_class': 'some class',
            'station_from_geo_id': 201,
            'station_to_geo_id': 202,
            'test_buckets': 'some buckets',
            'timestamp': 1585688400,
            'train_type': 'train_type',
            'user_device': 'some device',
            'yandex_uid': 'some uid',
            'bandit_type': 'fix13',
            'bandit_version': 444,
        }),
    )
