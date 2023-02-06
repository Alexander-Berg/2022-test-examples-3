# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import httpretty
from django.test import Client
from django.conf import settings
from hamcrest import assert_that, has_entries, contains

from common.models.tariffs import SuburbanTariffProvider, SuburbanSellingFlow, SuburbanSellingBarcodePreset

from travel.rasp.library.python.api_clients.travel_api.providers.movista import WicketTypeCode
from travel.rasp.library.python.api_clients.travel_api.client import TravelApiUserIdent, OrderState

from travel.rasp.suburban_selling.selling.aeroexpress.models import AeroexMenu
from travel.rasp.suburban_selling.tests.order.helpers import (
    register_travel_api_url, get_tvm_ticket_mock, get_user_from_blackbox_mock
)
from travel.rasp.suburban_selling.selling.movista.models import MovistaStations
from travel.rasp.suburban_selling.selling.order.order_info import (
    get_travel_api_order_info, OrderInfoCommand, ORDER_INFO_V3
)
from travel.rasp.suburban_selling.selling.movista.factories import MovistaStationsFactory
from travel.rasp.suburban_selling.selling.aeroexpress.factories import AeroexMenuFactory


def _register_get_state(httpretty, state, status_code=200):
    register_travel_api_url(
        httpretty,
        'get_order_state?order_id=order_id',
        status_code=status_code,
        response_json={'state': state}
    )


def _register_get_order(httpretty, state, provider=SuburbanTariffProvider.MOVISTA):
    response = {
        'state': state,
        'payment': {
            'current': {
                'payment_url': 'pay.url',
                'purchase_token': 'purchase_token_42',
            }
        },
        'order_price_info': {
            'price': {'value': 123.0}
        },
        'services': [{
            'suburban_info': {
                'provider': provider,
                'ticket_body': 'BODY',
                'ticket_number': '11111',
                'station_from_id': 100,
            }
        }]
    }

    service = response['services'][0]['suburban_info']
    if provider == SuburbanTariffProvider.MOVISTA:
        service['wicket'] = {
            'type': WicketTypeCode.TURNSTILE,
            'device_type': 'MID2Turnstile'
        }
        service['flow'] = SuburbanSellingFlow.VALIDATOR
        service['barcode_preset'] = SuburbanSellingBarcodePreset.PDF417_CPPK

    elif provider == SuburbanTariffProvider.IM:
        service['flow'] = SuburbanSellingFlow.SIMPLE
        service['barcode_preset'] = SuburbanSellingBarcodePreset.PDF417_SZPPK

    elif provider == SuburbanTariffProvider.AEROEXPRESS:
        service['flow'] = SuburbanSellingFlow.AEROEXPRESS
        service['barcode_preset'] = SuburbanSellingBarcodePreset.NO_BARCODE
        service['aeroexpress_info'] = {
            'ticket_url': 'ae_ticket_url',
            'tariff': 'ae_tariff',
            'st_depart': 'ae_route',
            'trip_date': '2022-03-01T00:00:00+03:00',
            'valid_until': '2022-04-01T00:00:00+03:00',
            'menu_id': 1
        }
        response['contact_info'] = {
            'email': 'ae_email',
            'phone': 'ae_phone'
        }
        response['serviced_at'] = '2022-03-01T01:00:00+03:00'

    register_travel_api_url(
        httpretty,
        'get_order?order_id=order_id',
        response_json=response
    )


def _register_start_payment(httpretty, status_code=200):
    register_travel_api_url(
        httpretty,
        'start_payment',
        status_code=status_code,
        request_json={
            'order_id': 'order_id',
            'return_url': settings.TRUST_RETURN_PATH,
            'source': 'mobile'
        }
    )


def _get_user_ident():
    return TravelApiUserIdent(
        session_id='uuid_value', yandex_uid='uuid_value', user_ticket='user_ticket', device_id='device'
    )


@httpretty.activate
def _check_simple_order_state(state, result_state=None, command=None):
    _register_get_state(httpretty, state)
    _register_start_payment(httpretty)
    user_ident = _get_user_ident()
    with get_tvm_ticket_mock():

        result_state = result_state or state
        order_info = get_travel_api_order_info(user_ident, 'order_id', command)
        assert order_info == {'status': result_state}

        requests = list(httpretty.latest_requests())
        if (
            (state == OrderState.RESERVED or state == OrderState.PAYMENT_FAILED) and
            command == OrderInfoCommand.START_PAYMENT
        ):
            assert len(requests) == 2
            assert 'get_order_state' in requests[0].path
            assert 'start_payment' in requests[1].path

        else:
            assert len(requests) == 1
            assert 'get_order_state' in requests[0].path


def test_get_travel_api_order_info_intermediate():
    for command in OrderInfoCommand.ALL | {None}:
        for state in OrderState.ALL:
            if not (
                (state == OrderState.WAITING_PAYMENT and command == OrderInfoCommand.GET_PAYMENT_DATA) or
                (state == OrderState.CONFIRMED and command == OrderInfoCommand.GET_TICKET)
            ):
                _check_simple_order_state(state, command=command)

        _check_simple_order_state('OTHER_STATE', result_state=OrderState.IN_PROGRESS, command=command)


@httpretty.activate
def test_get_travel_api_order_info_get_payment_data():
    _register_get_state(httpretty, OrderState.WAITING_PAYMENT)
    _register_get_order(httpretty, OrderState.WAITING_PAYMENT)
    user_ident = _get_user_ident()
    with get_tvm_ticket_mock():

        order_info = get_travel_api_order_info(user_ident, 'order_id', OrderInfoCommand.GET_PAYMENT_DATA)
        assert order_info == {
            'status': OrderState.WAITING_PAYMENT,
            'payment_url': 'pay.url',
            'purchase_token': 'purchase_token_42',
        }

        requests = httpretty.latest_requests()
        assert len(requests) == 2
        assert 'get_order_state' in requests[0].path
        assert 'get_order' in requests[1].path


@httpretty.activate
def test_get_travel_api_order_info_confirmed():
    _register_get_state(httpretty, OrderState.CONFIRMED)
    _register_get_order(httpretty, OrderState.CONFIRMED)

    AeroexMenu.objects.delete()
    AeroexMenuFactory()
    MovistaStations.objects().delete()
    MovistaStationsFactory(station_id=100, has_wicket=True, wicket_type='MID2Tutorial')

    user_ident = _get_user_ident()
    with get_tvm_ticket_mock():
        order_info = get_travel_api_order_info(user_ident, 'order_id', OrderInfoCommand.GET_TICKET)
        assert_that(order_info, has_entries({
            'status': OrderState.CONFIRMED,
            'ticket_body': 'BODY',
            'ticket_number': '11111',
            'flow': SuburbanSellingFlow.VALIDATOR,
            'barcode_preset': SuburbanSellingBarcodePreset.PDF417_CPPK,
            'price': 123.0,
            'wicket': has_entries({
                'type': WicketTypeCode.TURNSTILE,
                'device_type': 'MID2Turnstile'
            })
        }))

    requests = httpretty.latest_requests()
    assert len(requests) == 2
    assert 'get_order_state' in requests[0].path
    assert 'get_order' in requests[1].path

    _register_get_order(httpretty, OrderState.CONFIRMED, SuburbanTariffProvider.IM)
    with get_tvm_ticket_mock():
        order_info = get_travel_api_order_info(user_ident, 'order_id', OrderInfoCommand.GET_TICKET)
        assert_that(order_info, has_entries({
            'status': OrderState.CONFIRMED,
            'ticket_body': 'BODY',
            'ticket_number': '11111',
            'flow': SuburbanSellingFlow.SIMPLE,
            'barcode_preset': SuburbanSellingBarcodePreset.PDF417_SZPPK,
            'price': 123.0,
        }))
        assert 'wicket' not in order_info

    _register_get_order(httpretty, OrderState.CONFIRMED, SuburbanTariffProvider.AEROEXPRESS)
    with get_tvm_ticket_mock():
        order_info = get_travel_api_order_info(user_ident, 'order_id', OrderInfoCommand.GET_TICKET)
        assert_that(order_info, has_entries({
            'status': OrderState.CONFIRMED,
            'ticket_body': 'BODY',
            'ticket_number': 11111,
            'flow': SuburbanSellingFlow.AEROEXPRESS,
            'barcode_preset': SuburbanSellingBarcodePreset.NO_BARCODE,
            'price': 123.0,
            'payment_url': 'pay.url',
            'purchase_token': 'purchase_token_42',
            'create_dt': '2022-03-01T01:00:00+03:00',
            'passengers': contains(has_entries({
                'first_name': None,
                'surname': None,
                'patronymic_name': None,
                'ticket': has_entries({
                    'ticket_url': 'ae_ticket_url&type=html',
                    'code_url': 'ae_ticket_url&type=qr',
                    'price': 123.0,
                    'tariff': 'ae_tariff',
                    'route': 'ae_route',
                    'trip_date': '2022-03-01T00:00:00+03:00',
                    'dead_date': '2022-04-01T00:00:00+03:00',
                    'trip_count': 1,
                })
            }))
        }))


@httpretty.activate
def _check_order_info_view(
    state, command=None, response_json=None,
    get_state_status_code=200, start_payment_status_code=200, status_code=200, is_blackbox_error=False
):
    _register_get_state(httpretty, state, get_state_status_code)
    _register_start_payment(httpretty, start_payment_status_code)
    _register_get_order(httpretty, state)

    MovistaStations.objects().delete()
    MovistaStationsFactory(station_id=100, has_wicket=True, wicket_type='MID2Tutorial')

    with get_user_from_blackbox_mock(is_blackbox_error):
        with get_tvm_ticket_mock():
            params = {'uid': 'order_id', 'version': ORDER_INFO_V3}
            if command:
                params['command'] = command
            response = Client().get(
                '/order_info/', params,
                **{'HTTP_X_YA_UUID': 'uuid_value'}
            )

            assert response.status_code == status_code
            order_info = json.loads(response.content)
            if response_json:
                assert order_info == response_json
            elif status_code == 200:
                assert order_info == {'status': state}


def test_get_order_info():
    _check_order_info_view(OrderState.NEW)
    _check_order_info_view(OrderState.IN_PROGRESS, command=OrderInfoCommand.START_PAYMENT)
    _check_order_info_view(OrderState.RESERVED)
    _check_order_info_view(OrderState.RESERVED, command=OrderInfoCommand.GET_PAYMENT_DATA)
    _check_order_info_view(OrderState.RESERVED, command=OrderInfoCommand.GET_TICKET)
    _check_order_info_view(OrderState.RESERVED, command=OrderInfoCommand.START_PAYMENT)
    _check_order_info_view(OrderState.RESERVED, command=OrderInfoCommand.START_PAYMENT, start_payment_status_code=409)

    _check_order_info_view(OrderState.CANCELLED)
    _check_order_info_view('OTHER_STATE', response_json={'status': OrderState.IN_PROGRESS})

    _check_order_info_view(OrderState.CONFIRMED, command=OrderInfoCommand.GET_TICKET, response_json={
        'status': OrderState.CONFIRMED,
        'ticket_body': 'BODY',
        'ticket_number': '11111',
        'flow': SuburbanSellingFlow.VALIDATOR,
        'barcode_preset': SuburbanSellingBarcodePreset.PDF417_CPPK,
        'price': 123.0,
        'wicket': {
            'type': WicketTypeCode.TURNSTILE,
            'device_type': 'MID2Turnstile'
        }
    })

    _check_order_info_view(OrderState.WAITING_PAYMENT)
    _check_order_info_view(OrderState.WAITING_PAYMENT, command=OrderInfoCommand.START_PAYMENT)
    _check_order_info_view(OrderState.WAITING_PAYMENT, command=OrderInfoCommand.GET_TICKET)
    _check_order_info_view(OrderState.WAITING_PAYMENT, command=OrderInfoCommand.GET_PAYMENT_DATA, response_json={
        'status': OrderState.WAITING_PAYMENT,
        'payment_url': 'pay.url',
        'purchase_token': 'purchase_token_42',
    })

    _check_order_info_view(
        'NEW', command='unknown',
        status_code=400, response_json={'error': 'Unknown command "unknown"', 'status': 'failed'}
    )

    _check_order_info_view(
        OrderState.NEW, is_blackbox_error=True,
        status_code=403, response_json={'status': 'failed', 'error': 'Can not get an user ticket from blackbox'}
    )

    _check_order_info_view(OrderState.NEW, get_state_status_code=401, status_code=401)
    _check_order_info_view(
        OrderState.WAITING_PAYMENT, get_state_status_code=403, status_code=403,
        command=OrderInfoCommand.GET_PAYMENT_DATA
    )
    _check_order_info_view(OrderState.NEW, get_state_status_code=404, status_code=404)
    _check_order_info_view(
        OrderState.RESERVED, start_payment_status_code=401, status_code=401,
        command=OrderInfoCommand.START_PAYMENT
    )
