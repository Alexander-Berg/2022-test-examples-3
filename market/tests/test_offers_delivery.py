# -*- coding: utf-8 -*-

import flask
import pytest

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage
from market.idx.pylibrary.regional_delivery.options import DeliveryOption, OptionType
from market.proto.delivery.delivery_calc import delivery_calc_pb2
from market.proto.feedparser.deprecated import OffersData_pb2


class StorageMock(Storage):
    @staticmethod
    def get_published_offer_by_id(feed_id, offer_id):
        if feed_id != '1069' or offer_id != '1':
            return None

        offer_info = OffersData_pb2.Offer()
        offer_info.Comment = 'some text comment'
        offer_info.DeliveryOptions.add()
        offer_info.DeliveryOptions[0].Cost = 300.0
        offer_info.DeliveryOptions[0].DaysMax = 2
        offer_info.shop_name = 'Я Тестовый шоп 4 edit 21^39 090117'
        offer_info.price_expression = '3.000000 1 0 RUR RUR'
        offer_info.yx_shop_name = 'Я Тестовый шоп 4 edit 21^39 090117'
        offer_info.yx_shop_offer_id = '1'

        return {
            "offer_id": "1",
            "data:offer": offer_info,
        }

    @staticmethod
    def get_raw_delivery_info(offer):
        if not offer or offer.get("offer_id") != '1':
            return {
                'buckets': [],
                'option_groups': [],
                'pickup_buckets': []
            }

        bucket = delivery_calc_pb2.DeliveryOptionsBucket()
        bucket.delivery_opt_bucket_id = 2072001
        bucket.currency = 'RUR'
        bucket.carrier_ids.append(99)
        reg = bucket.delivery_option_group_regs.add()
        reg.region = 213
        reg.option_type = delivery_calc_pb2.NORMAL_OPTION
        reg.delivery_opt_group_id = 12345

        option_group = delivery_calc_pb2.DeliveryOptionsGroup()
        option_group.delivery_option_group_id = 12345
        opt = option_group.delivery_options.add()
        opt.delivery_cost = 100
        opt.order_before = 23
        opt.max_days_count = 10
        opt.min_days_count = 3

        pickup_bucket = delivery_calc_pb2.PickupBucket()
        pickup_bucket.bucket_id = 100200
        pickup_bucket.program = delivery_calc_pb2.MARKET_DELIVERY_PROGRAM
        pickup_bucket.currency = 'RUR'
        pickup_bucket.carrier_ids.append(107)
        outlet = pickup_bucket.delivery_option_group_outlets.add()
        outlet.outlet_id = 2001
        outlet.option_group_id = 2

        return {
            'buckets': [bucket],
            'option_groups': [option_group],
            'pickup_buckets': [pickup_bucket]
        }

    @staticmethod
    def get_regional_delivery_options(offer, regions):
        def get_options(region):
            if region == '213':
                opt = delivery_calc_pb2.DeliveryOption()
                opt.delivery_cost = 10000
                opt.max_days_count = 10
                opt.min_days_count = 3
                opt.order_before = 23
                return DeliveryOption(OptionType.NORMAL, opt, 213, 'RUR')
            elif region == '26':
                return DeliveryOption(OptionType.UNSPECIFIED)
            else:
                return DeliveryOption(OptionType.FORBIDDEN)

        if not offer or offer.get("offer_id") != '1':
            return {}
        result = {}
        for r in regions:
            result[r] = get_options(r)
        return result


@pytest.fixture(scope='module')
def test_app():
    return create_flask_app(StorageMock())


def check_raw_delivery(actual_data, self_url, status_code):
    expected_data = {
        'status_code': status_code,
        'buckets': [
            {
                'carrierIds': [
                    99
                ],
                'currency': 'RUR',
                'deliveryOptBucketId': '2072001',
                'deliveryOptionGroupRegs': [
                    {
                        'region': 213,
                        'deliveryOptGroupId': '12345',
                        'optionType': 'NORMAL_OPTION',
                    },
                ],
            },
        ],
        'option_groups': [
            {
                'deliveryOptionGroupId': '12345',
                'deliveryOptions': [
                    {
                        'deliveryCost': '100',
                        'minDaysCount': 3,
                        'maxDaysCount': 10,
                        'orderBefore': 23,
                    },
                ],
            },
        ],
        'pickup_buckets': [
            {
                'bucketId': '100200',
                'currency': 'RUR',
                'program': 'MARKET_DELIVERY_PROGRAM',
                'carrierIds': [
                    107
                ],
                'deliveryOptionGroupOutlets': [
                    {
                        'outletId': '2001',
                        'optionGroupId': '2'
                    }
                ]
            }
        ],
        'self_url': self_url
    }
    assert expected_data == actual_data


def test_get_raw_delivery(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1/delivery')
        actual_data = flask.json.loads(resp.data)
        actual_data['status_code'] = resp.status_code
        url = 'http://localhost:29334/v1/feeds/1069/sessions/published/offers/1/delivery'

        check_raw_delivery(actual_data, url, 200)


def test_get_offer_id_published_session_not_found(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1070/sessions/published/offers/1/delivery')
        assert resp.status_code == 404


def test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/feeds/1069/sessions/published/offers/1/delivery?format=json'):
        assert flask.request.path == '/v1/feeds/1069/sessions/published/offers/1/delivery'
        assert flask.request.args['format'] == 'json'


def test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1/delivery')
        assert resp.status_code == 200
        assert resp.headers['Content-type'] == 'application/json; charset=utf-8'


def test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1/delivery?format=json')
        assert resp.status_code == 200
        assert resp.headers['Content-type'] == 'application/json; charset=utf-8'
        assert resp.data


def test_get_delivery_cgi_one_region_allowed(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/sessions/published/offers/1/delivery?rids=213,4,26')

        expected_data = {
            "status_code": 200,
            "regional_delivery_options": [
                {
                    "delivery": {
                        "allowed": True,
                        "options": {},
                    },
                    "requested_region_id": 26
                },
                {
                    "delivery": {
                        "allowed": True,
                        "options": {
                            "dayFrom": 3,
                            "dayTo": 10,
                            "order_before": 23,
                            "price": {
                                "currency": "RUR",
                                "value": 100
                            }
                        },
                        "region_id": 213
                    },
                    "requested_region_id": 213
                },
                {
                    "delivery": {
                        "allowed": False,
                        "options": {},
                    },
                    "requested_region_id": 4
                }
            ],
            "self_url": "http://localhost:29334/v1/feeds/1069/sessions/published/offers/1/delivery"
        }

        actual_data = flask.json.loads(resp.data)
        actual_data["status_code"] = resp.status_code

        assert expected_data['status_code'] == actual_data['status_code']
        assert expected_data['self_url'] == actual_data['self_url']
        assert sorted(
            expected_data['regional_delivery_options'],
            key=lambda option: option['requested_region_id']
        ) == sorted(
            actual_data['regional_delivery_options'],
            key=lambda option: option['requested_region_id']
        )
