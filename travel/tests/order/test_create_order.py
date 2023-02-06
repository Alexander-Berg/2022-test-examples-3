# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from copy import deepcopy

import httpretty
import mock
import pytest
from django.conf import settings
from django.test import Client
from django.utils.http import urlencode
from hamcrest import assert_that, has_entries, contains, anything

from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.api_clients.travel_api.providers.movista import WicketTypeCode

from travel.rasp.suburban_selling.selling.movista.factories import MovistaStationsFactory
from travel.rasp.suburban_selling.selling.movista.models import MovistaStations
from travel.rasp.suburban_selling.selling.order.create_order import CREATE_ORDER_V3
from travel.rasp.suburban_selling.tests.order.helpers import get_user_from_blackbox_mock, get_tvm_ticket_mock


pytestmark = [pytest.mark.dbuser]


CREATE_ORDER_BASE_REQUEST = {
    'station_from': 501,
    'station_to': 502,
    'departure_date': '2018-07-15T00:00:00+03:00',
    'user_info': json.dumps({
        'ip': '1.2.3.4',
        'region_id': 213,
        'phone': '+7(123) 456-78-90 ',
        'email': 'user@example.org',
    }),
    'uuid': 'uuid_value',
    'device_id': 'device_id_value',
    'version': CREATE_ORDER_V3,
    'price': 399.9,
}


CREATE_AEROEXPRESS_ORDER_REQUEST = dict(CREATE_ORDER_BASE_REQUEST.items() + {
    'provider': 'aeroexpress',
    'partner': 'aeroexpress',
    'book_data': json.dumps({
        'menu_id': 14,
        'order_type': 25
    })
}.items())


CREATE_MOVISTA_ORDER_REQUEST = dict(CREATE_ORDER_BASE_REQUEST.items() + {
    'provider': 'movista',
    'partner': 'cppk',
    'book_data': json.dumps({
        'fare_id': 333,
        'station_from_express_id': '4242',
        'station_to_express_id': '4343',
        'date': '2018-07-15'
    })
}.items())


def get_send_order_label_to_redir_mock():
    return mock.patch(
        'travel.rasp.suburban_selling.selling.order.create_order.get_label_hash_from_redir',
        return_value='hash'
    )


@httpretty.activate
@replace_now('2018-07-16')
def test_create_travel_api_order():
    create_station(id=501)
    create_station(id=502)
    MovistaStations.objects().delete()
    MovistaStationsFactory(station_id=501, has_wicket=True, wicket_type='MID2Tutorial')

    def request_callback(request, uri, response_headers):
        request_json = json.loads(request.body)
        assert_that(request_json, has_entries({
            'deduplication_key': anything(),
            'contact_info': has_entries({
                'email': 'user@example.org',
                'phone': '71234567890' if has_extra_user_info else settings.SELLING_SUPPORT_TELEPHONE
            }),
            'user_info': has_entries({
                'ip': '1.2.3.4' if has_extra_user_info else None,
                'geo_id': 213 if has_extra_user_info else 0,
            }),
            'order_history': [],
            'label': 'hash',
            'suburban_services': contains(has_entries({
                'provider': 'movista',
                'station_from_id': 501,
                'station_to_id': 502,
                'price': 399.9,
                'carrier_partner': 'cppk',
                'provider_book_data': has_entries({
                    'date': '2018-07-15',
                    'station_from_express_id': 4242,
                    'station_to_express_id': 4343,
                    'fare_id': 333,
                    'wicket': has_entries({
                        'type': WicketTypeCode.TURNSTILE,
                        'device_type': 'MID2Turnstile'
                    }),
                }),
                'test_context_token': 'test_token'
            })),
            'payment_test_context_token': 'payment_token'
        }))

        assert request.headers.get('Content-Type') == 'application/json'
        assert request.headers.get('X-Ya-Service-Ticket') == 'tvm_ticket'
        assert request.headers.get('X-Ya-Session-Key') == 'uuid_value'
        assert request.headers.get('X-Ya-YandexUid') == 'uuid_value'
        assert request.headers.get('X-Ya-User-Ticket') == 'user_ticket'
        assert request.headers.get('X-Ya-User-Agent') == 'user_agent'
        assert request.headers.get('X-Ya-User-Ip') == 'user_ip'

        response_json = {'id': 'order_id', 'state': 'RESERVED'}

        return [200, response_headers, json.dumps(response_json)]

    httpretty.register_uri(
        httpretty.POST, '{}{}'.format(settings.TRAVEL_API_URL, 'generic_booking_flow/v1/create_order'),
        body=request_callback, content_type='application/json'
    )

    with get_user_from_blackbox_mock():
        with get_tvm_ticket_mock():
            with get_send_order_label_to_redir_mock():
                headers = {
                    'HTTP_X_YA_UUID': 'uuid_value',
                    'HTTP_USER_AGENT': 'user_agent',
                    'HTTP_X_REAL_IP': 'user_ip',
                    'HTTP_X_TEST_CONTEXT_TOKEN': 'test_token',
                    'HTTP_X_PAYMENT_TEST_CONTEXT_TOKEN': 'payment_token',
                }

                has_extra_user_info = True
                request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
                response = Client().post(
                    '/create_order/', urlencode(request_body),
                    content_type='application/x-www-form-urlencoded',
                    **headers
                )

                assert response.status_code == 200
                data = json.loads(response.content)
                assert data['uid'] == 'order_id'
                assert data['status'] == 'RESERVED'

                has_extra_user_info = False
                request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
                request_body['user_info'] = json.dumps({'email': 'user@example.org'})

                response = Client().post(
                    '/create_order/', urlencode(request_body),
                    content_type='application/x-www-form-urlencoded',
                    **headers
                )

                assert response.status_code == 200
                data = json.loads(response.content)
                assert data['uid'] == 'order_id'
                assert data['status'] == 'RESERVED'


@httpretty.activate
@replace_now('2018-07-16')
def test_create_travel_api_aeroexpress_order():
    create_station(id=501)
    create_station(id=502)

    def request_callback(request, uri, response_headers):
        request_json = json.loads(request.body)
        assert_that(request_json, has_entries({
            'deduplication_key': anything(),
            'contact_info': has_entries({
                'email': 'user@example.org',
                'phone': '71234567890'
            }),
            'user_info': has_entries({
                'ip': '1.2.3.4',
                'geo_id': 213,
            }),
            'order_history': [],
            'label': 'hash',
            'suburban_services': contains(has_entries({
                'provider': 'aeroexpress',
                'station_from_id': 501,
                'station_to_id': 502,
                'price': 399.9,
                'carrier_partner': 'aeroexpress',
                'provider_book_data': has_entries({
                    'menu_id': 14,
                    'order_type': 25
                }),
                'test_context_token': 'test_token'
            })),
            'payment_test_context_token': 'payment_token'
        }))

        assert request.headers.get('Content-Type') == 'application/json'
        assert request.headers.get('X-Ya-Service-Ticket') == 'tvm_ticket'
        assert request.headers.get('X-Ya-Session-Key') == 'uuid_value'
        assert request.headers.get('X-Ya-YandexUid') == 'uuid_value'
        assert request.headers.get('X-Ya-User-Ticket') == 'user_ticket'
        assert request.headers.get('X-Ya-User-Agent') == 'user_agent'
        assert request.headers.get('X-Ya-User-Ip') == 'user_ip'

        response_json = {'id': 'order_id', 'state': 'RESERVED'}

        return [200, response_headers, json.dumps(response_json)]

    httpretty.register_uri(
        httpretty.POST, '{}{}'.format(settings.TRAVEL_API_URL, 'generic_booking_flow/v1/create_order'),
        body=request_callback, content_type='application/json'
    )

    with get_user_from_blackbox_mock():
        with get_tvm_ticket_mock():
            with get_send_order_label_to_redir_mock():
                headers = {
                    'HTTP_X_YA_UUID': 'uuid_value',
                    'HTTP_USER_AGENT': 'user_agent',
                    'HTTP_X_REAL_IP': 'user_ip',
                    'HTTP_X_TEST_CONTEXT_TOKEN': 'test_token',
                    'HTTP_X_PAYMENT_TEST_CONTEXT_TOKEN': 'payment_token',
                }
                request_body = deepcopy(CREATE_AEROEXPRESS_ORDER_REQUEST)
                response = Client().post(
                    '/create_order/', urlencode(request_body),
                    content_type='application/x-www-form-urlencoded',
                    **headers
                )

                assert response.status_code == 200
                data = json.loads(response.content)
                assert data['uid'] == 'order_id'
                assert data['status'] == 'RESERVED'


def _register_travel_api_create_order_url(httpretty, response_json=None, status_code=200):
    if not response_json:
        response_json = {'id': 'order_id', 'state': 'CONFIRMED'}
    httpretty.register_uri(
        httpretty.POST, '{}{}'.format(settings.TRAVEL_API_URL, 'generic_booking_flow/v1/create_order'),
        status=status_code, body=json.dumps(response_json), content_type='application/json'
    )


def _get_create_order_response(request_body, use_uuid=True):
    meta = {'HTTP_X_YA_UUID': 'uuid_value'} if use_uuid else {}
    return Client().post(
        '/create_order/', urlencode(request_body),
        content_type='application/x-www-form-urlencoded',
        **meta
    )


@httpretty.activate
@replace_now('2018-07-16')
def test_create_travel_api_order_error():
    create_station(id=501), create_station(id=502)
    MovistaStations.objects().delete()
    MovistaStationsFactory(station_id=501, has_wicket=True, wicket_type='MID2Tutorial')

    with get_send_order_label_to_redir_mock():
        with get_tvm_ticket_mock():
            _register_travel_api_create_order_url(httpretty)
            request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
            response = _get_create_order_response(request_body, use_uuid=False)
            assert response.status_code == 401

            with replace_dynamic_setting('SUBURBAN_SELLING__MOVISTA_ORDER_ENABLED', False):
                response = _get_create_order_response(request_body)
                assert response.status_code == 403

            request_body['provider'] = 'unknown'
            response = _get_create_order_response(request_body)
            assert response.status_code == 400

            request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
            request_body['book_data'] = 'xxx'
            response = _get_create_order_response(request_body)
            assert response.status_code == 400

            request_body.pop('book_data')
            response = _get_create_order_response(request_body)
            assert response.status_code == 400

            _register_travel_api_create_order_url(httpretty, status_code=401)
            request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
            response = _get_create_order_response(request_body)
            assert response.status_code == 401

            _register_travel_api_create_order_url(httpretty, status_code=403)
            request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
            response = _get_create_order_response(request_body)
            assert response.status_code == 403

            _register_travel_api_create_order_url(httpretty, status_code=404)
            request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
            response = _get_create_order_response(request_body)
            assert response.status_code == 404

        with get_user_from_blackbox_mock(is_blackbox_error=True):
            _register_travel_api_create_order_url(httpretty)
            request_body = deepcopy(CREATE_MOVISTA_ORDER_REQUEST)
            response = _get_create_order_response(request_body)
            assert response.status_code == 403
