# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import httpretty
from django.test import Client
from hamcrest import assert_that, contains_inanyorder, has_entries

import travel.rasp.suburban_selling.selling.order.orders_list_info  # noqa
from travel.rasp.suburban_selling.tests.order.helpers import (
    register_travel_api_url, get_tvm_ticket_mock, get_user_from_blackbox_mock
)
from travel.rasp.library.python.api_clients.travel_api.client import OrderState


@httpretty.activate
def test_orders_list_info():
    register_travel_api_url(
        httpretty,
        'get_order_state_batch',
        request_json={
            'order_ids': [
                'aeroexpress_order_1_uid', 'aeroexpress_order_2_uid', 'aeroexpress_order_3_uid',
                'movista_order_1_uid', 'movista_order_2_uid'
            ]
        },
        response_json={
            'orders': [
                {'order_id': 'aeroexpress_order_1_uid', 'state': OrderState.CONFIRMED},
                {'order_id': 'aeroexpress_order_2_uid', 'state': OrderState.RESERVED},
                {'order_id': 'aeroexpress_order_3_uid', 'state': OrderState.WAITING_PAYMENT},
                {'order_id': 'movista_order_1_uid', 'state': OrderState.CONFIRMED},
                {'order_id': 'movista_order_2_uid', 'state': OrderState.WAITING_PAYMENT},
            ]
        }
    )

    request = {
        'orders': [
            {'uid': 'aeroexpress_order_1_uid', 'provider': 'aeroexpress'},
            {'uid': 'aeroexpress_order_2_uid', 'provider': 'aeroexpress'},
            {'uid': 'aeroexpress_order_3_uid', 'provider': 'aeroexpress'},
            {'uid': 'movista_order_1_uid', 'provider': 'movista'},
            {'uid': 'movista_order_2_uid', 'provider': 'movista'},
        ]
    }
    with get_user_from_blackbox_mock():
        with get_tvm_ticket_mock():
            response = Client().post(
                '/orders_list_info/', json.dumps(request),
                content_type='application/json', **{'HTTP_X_YA_UUID': 'uuid_value'}
            )

            assert response.status_code == 200

            data = json.loads(response.content)
            assert_that(data, has_entries({
                'orders': contains_inanyorder(
                    has_entries({'uid': 'aeroexpress_order_1_uid', 'status': OrderState.CONFIRMED}),
                    has_entries({'uid': 'aeroexpress_order_2_uid', 'status': OrderState.RESERVED}),
                    has_entries({'uid': 'aeroexpress_order_3_uid', 'status': OrderState.WAITING_PAYMENT}),
                    has_entries({'uid': 'movista_order_1_uid', 'status': OrderState.CONFIRMED}),
                    has_entries({'uid': 'movista_order_2_uid', 'status': OrderState.WAITING_PAYMENT}),
                )
            }))


@httpretty.activate
def test_orders_list_info_error():
    register_travel_api_url(
        httpretty, 'get_order_state_batch',
        request_json={'order_ids': ['uid']}, status_code=403
    )

    with get_user_from_blackbox_mock():
        with get_tvm_ticket_mock():
            request = {'orders': [{'uid': 'uid', 'provider': 'movista'}]}
            response = Client().post(
                '/orders_list_info/', json.dumps(request),
                content_type='application/json', **{'HTTP_X_YA_UUID': 'uuid_value'}
            )
            assert response.status_code == 403

            response = Client().post(
                '/orders_list_info/', json.dumps({'wrong': 'format'}),
                content_type='application/json', **{'HTTP_X_YA_UUID': 'uuid_value'}
            )
            assert response.status_code == 400
