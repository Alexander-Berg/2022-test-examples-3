# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import httpretty
from django.conf import settings
from django.test import Client
from django.utils.http import urlencode

import travel.rasp.suburban_selling.selling.order.user_action_event  # noqa
from common.tester.utils.datetime import replace_now
from travel.rasp.suburban_selling.tests.order.helpers import (
    register_travel_api_url, get_user_from_blackbox_mock, get_tvm_ticket_mock
)


def _register_get_order(httpretty):
    register_travel_api_url(
        httpretty, 'get_order?order_id=order_id',
        response_json={
            'services': [{
                'suburban_info': {
                    'ticket_body': 'ticket',
                    'partner_order_id': 111,
                }
            }]
        }
    )


def _register_activation(httpretty):
    def request_callback(request, uri, response_headers):
        request_json = json.loads(request.body)
        assert request_json == {
            'orderId': 111,
            'date': '2021-03-10T12:30:00',
            'success': True,
            'qrBody': 'QR',
            'ticketBody': 'ticket'
        }
        response = {
            'result': 'SUCCESS',
            'activationId': 222
        }
        return [200, response_headers, json.dumps(response)]

    httpretty.register_uri(
        httpretty.POST,
        '{}api/v1/activation'.format(settings.MOVISTA_API_HOST),
        content_type='application/json',
        body=request_callback
    )


@replace_now('2021-03-10 12:30:00')
@httpretty.activate
def test_user_action_event():
    _register_get_order(httpretty)
    _register_activation(httpretty)

    with get_user_from_blackbox_mock():
        with get_tvm_ticket_mock():
            request_body = {'uid': 'order_id', 'is_success': True, 'qr_body': 'QR'}
            response = Client().post(
                '/user_action_event/',
                data=urlencode(request_body),
                content_type='application/x-www-form-urlencoded',
                **{'HTTP_X_YA_UUID': 'uuid_value'}
            )

            assert response.status_code == 200
            data = json.loads(response.content)
            assert data == {
                'result': 'SUCCESS',
                'activation_id': 222
            }
