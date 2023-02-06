# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

import re
from decimal import Decimal

import mock
import pytest
from django.test import Client
from httpretty import Response

from common.dynamic_settings.default import conf
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.insurance.checkout import IM_INSURANCE_CHECKOUT_METHOD
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PartnerDataFactory, PassengerFactory, InsuranceFactory
)
from travel.rasp.train_api.train_purchase.views.insurance import run_process
from travel.rasp.train_api.train_purchase.workflow.booking import TRAIN_BOOKING_PROCESS

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


IM_INSURANCE_CHECKOUT_RESPONSE = [
    Response(
        body="""
{
  "OrderId": 333,
  "OrderItemId": 568141,
  "Amount": 100.0,
  "OrderCustomerId": 831463
}
        """
    ),
    Response(
        body="""
{
  "OrderId": 333,
  "OrderItemId": 568142,
  "Amount": 70.0,
  "OrderCustomerId": 831464
}
        """
    )
]

IM_INSURANCE_CHECKOUT_RESPONSE_INVALID = [
    Response(
        body='testing error',
        status=500,
    ),
]


def _create_order(insurance_enabled=True):
    return TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id='333', operation_id='568141'),
        insurance_enabled=insurance_enabled,
        process={'suspended': True},
        passengers=[
            PassengerFactory(
                customer_id='831463',
                insurance=InsuranceFactory(
                    amount=Decimal('100.0'),
                )
            ),
            PassengerFactory(
                customer_id='831464',
                insurance=InsuranceFactory(
                    amount=Decimal('70.0'),
                )
            ),
        ],
    )


@pytest.fixture
def m_run_process():
    with mock.patch.object(run_process, 'apply_async', autospec=True, return_value=None) as m_run_process:
        yield m_run_process


def test_im_checkout_ok(m_run_process, httpretty):
    mock_im(httpretty, IM_INSURANCE_CHECKOUT_METHOD, responses=IM_INSURANCE_CHECKOUT_RESPONSE)
    order = _create_order()
    response = Client().get('/ru/api/train-purchase/orders/insurance/{}/checkout/?enabled=true'.format(order.uid))

    assert response.status_code == 200
    assert len(httpretty.latest_requests) > 0
    m_run_process.assert_called_once_with([TRAIN_BOOKING_PROCESS, str(order.id), {'order_uid': order.uid}])


def test_im_checkout_cancel(m_run_process, httpretty):
    mock_im(httpretty, IM_INSURANCE_CHECKOUT_METHOD, responses=IM_INSURANCE_CHECKOUT_RESPONSE)
    order = _create_order()
    response = Client().get('/ru/api/train-purchase/orders/insurance/{}/checkout/'.format(order.uid))

    assert response.status_code == 200
    assert len(httpretty.latest_requests) == 0
    m_run_process.assert_called_once_with([TRAIN_BOOKING_PROCESS, str(order.id), {'order_uid': order.uid}])


def test_im_checkout_disabled(m_run_process, httpretty):
    mock_im(httpretty, IM_INSURANCE_CHECKOUT_METHOD, responses=IM_INSURANCE_CHECKOUT_RESPONSE)
    order = _create_order(insurance_enabled=False)
    response = Client().get('/ru/api/train-purchase/orders/insurance/{}/checkout/?enabled=true'.format(order.uid))

    assert response.status_code == 409
    assert len(httpretty.latest_requests) == 0
    assert m_run_process.call_count == 0


def test_im_checkout_fail(m_run_process, httpretty):
    mock_im(httpretty, IM_INSURANCE_CHECKOUT_METHOD, responses=IM_INSURANCE_CHECKOUT_RESPONSE_INVALID)
    httpretty.register_uri(httpretty.GET, re.compile(r'https?://.*sender.yandex-team.ru.*'))

    order = _create_order()
    response = Client().get('/ru/api/train-purchase/orders/insurance/{}/checkout/?enabled=true'.format(order.uid))

    assert response.status_code == 500
    assert conf.TRAIN_PURCHASE_ERRORS_EMAIL in httpretty.last_request.querystring['to_email']
    assert m_run_process.call_count == 0
