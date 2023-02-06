# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework.test import APIRequestFactory

from common.tester.factories import create_station
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PaymentFactory
from travel.rasp.train_api.train_purchase.utils.decorators import order_view


def call_api_order_view(uid):
    inner_mock = mock.Mock(__name__=b'inner_mock', return_value=Response())
    actual_order_view = order_view()
    view = api_view()(actual_order_view(inner_mock))
    return view(APIRequestFactory().get('/'), uid=uid), inner_mock


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_order_view():
    order = TrainOrderFactory()
    response, inner_mock = call_api_order_view(order.uid)

    assert response.status_code == 200
    inner_mock.assert_called_once_with(mock.ANY, order)


def test_order_view_not_found():
    response, inner_mock = call_api_order_view('invalid_uid')

    assert response.status_code == 404
    assert response.data == {'errors': {'uid': 'Order was not found'}}
    inner_mock.assert_not_called()


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_order_view_unhandled_exception_state_booking():
    station_from = create_station()
    station_to = create_station()
    order = TrainOrderFactory(station_from=station_from, station_to=station_to,
                              process={'state': 'unhandled_exception_state'})
    response, inner_mock = call_api_order_view(order.uid)

    assert response.status_code == 500
    assert response.data == {
        'errors': {
            'process_exception_state': {
                'type': 'process_exception_state',
                'data': {'orderNumber': None},
                'message': None
            }
        }
    }
    inner_mock.assert_not_called()

    order = TrainOrderFactory(station_from=station_from, station_to=station_to,
                              process={'state': 'unhandled_exception_state'}, partner_data=dict(order_num='100500'))
    response, inner_mock = call_api_order_view(order.uid)

    assert response.status_code == 500
    assert response.data == {
        'errors': {
            'process_exception_state': {
                'type': 'process_exception_state',
                'data': {'orderNumber': '100500'},
                'message': None
            }
        }
    }
    inner_mock.assert_not_called()


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_order_view_unhandled_exception_state_payment():
    station_from = create_station()
    station_to = create_station()
    order = TrainOrderFactory(station_from=station_from, station_to=station_to, partner_data=dict(order_num='200500'))
    PaymentFactory(order_uid=order.uid, process={'state': 'unhandled_exception_state'})

    response, inner_mock = call_api_order_view(order.uid)

    assert response.status_code == 500
    assert response.data == {
        'errors': {
            'process_exception_state': {
                'type': 'process_exception_state',
                'data': {'orderNumber': '200500'},
                'message': None
            }
        }
    }
    inner_mock.assert_not_called()
