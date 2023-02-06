# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import mock

from django.conf import settings

from common.utils.blackbox_wrapper import SessionInvalid


def register_travel_api_url(httpretty, endpoint, response_json=None, request_json=None, status_code=200):
    def request_callback(request, uri, response_headers):
        assert request.headers.get('X-Ya-Service-Ticket') == 'tvm_ticket'
        assert request.headers.get('X-Ya-Session-Key') == 'uuid_value'
        assert request.headers.get('X-Ya-YandexUid') == 'uuid_value'
        assert request.headers.get('X-Ya-User-Ticket') == 'user_ticket'
        if response_json:
            assert request.headers.get('Content-Type') == 'application/json'
        if request_json:
            assert json.loads(request.body) == request_json

        response_body = json.dumps(response_json) if response_json else 'OK'
        return [status_code, response_headers, response_body]

    method = httpretty.POST if request_json else httpretty.GET
    httpretty.register_uri(
        method, '{}generic_booking_flow/v1/{}'.format(settings.TRAVEL_API_URL, endpoint),
        body=request_callback, content_type='application/json'
    )


def get_tvm_ticket_mock():
    return mock.patch(
        'common.data_api.tvm.header_creator.TvmHeaderCreator.get_headers',
        return_value={'X-Ya-Service-Ticket': 'tvm_ticket'}
    )


def get_user_from_blackbox_mock(is_blackbox_error=False):
    blackbox_fun_path = 'travel.rasp.suburban_selling.selling.order.helpers.get_user_from_blackbox'
    if not is_blackbox_error:
        return mock.patch(blackbox_fun_path, return_value=('12345', 'user_ticket'))
    else:
        return mock.patch(blackbox_fun_path, side_effect=SessionInvalid)
