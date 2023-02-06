# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest
from hamcrest import assert_that, contains, has_properties
from httpretty import Response

from travel.rasp.train_api.train_partners.base.insurance.checkout import checkout, InsuranceCheckoutException
from travel.rasp.train_api.train_partners.im.base import ImError
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.insurance.checkout import IM_INSURANCE_CHECKOUT_METHOD
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PartnerDataFactory, PassengerFactory, InsuranceFactory
)
from travel.rasp.train_api.train_purchase.core.models import TrainPartner


IM_INSURANCE_CHECKOUT_RESPONSE = [
    Response(
        body="""
{
  "OrderId": 333,
  "OrderItemId": 568142,
  "Amount": 100.0,
  "OrderCustomerId": 831463
}
        """
    ),
    Response(
        body="""
{
  "OrderId": 333,
  "OrderItemId": 568143,
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


def _create_order():
    return TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id='333', operation_id='568141'),
        passengers=[
            PassengerFactory(
                customer_id='831463',
                insurance=InsuranceFactory(
                    amount=Decimal('100.0'),
                    company='Renessans',
                    package='AccidentWithFloatPremium',
                    provider='P3',
                )
            ),
            PassengerFactory(
                customer_id='831464',
                insurance=InsuranceFactory(
                    amount=Decimal('70.0'),
                    company='Renessans',
                    package='AccidentWithFloatPremium',
                    provider='P3',
                )
            ),
        ],
    )


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_checkout(httpretty):
    mock_im(httpretty, IM_INSURANCE_CHECKOUT_METHOD, responses=IM_INSURANCE_CHECKOUT_RESPONSE)
    order = _create_order()

    checkout(order)
    order.reload()

    assert_that(httpretty.latest_requests, contains(
        has_properties(parsed_body={
            'AgentPaymentId': None,
            'Company': 'Renessans',
            'Provider': 'P3',
            'Product': {
                '$type':
                    'ApiContracts.Insurance.V1.Messages.Travel.RailwayInsuranceTravelCheckoutRequest, ApiContracts',
                'Package': 'AccidentWithFloatPremium',
            },
            'MainServiceReference': {
                '$type': 'ApiContracts.Insurance.V1.Messages.Travel.MainServiceReferenceInternal, ApiContracts',
                'OrderCustomerId': 831463,
                'OrderItemId': 568141,
            },
        }),
        has_properties(parsed_body={
            'AgentPaymentId': None,
            'Company': 'Renessans',
            'Provider': 'P3',
            'Product': {
                '$type':
                    'ApiContracts.Insurance.V1.Messages.Travel.RailwayInsuranceTravelCheckoutRequest, ApiContracts',
                'Package': 'AccidentWithFloatPremium',
            },
            'MainServiceReference': {
                '$type': 'ApiContracts.Insurance.V1.Messages.Travel.MainServiceReferenceInternal, ApiContracts',
                'OrderCustomerId': 831464,
                'OrderItemId': 568141,
            },
        }),
    ))

    assert_that(order.passengers, contains(
        has_properties(
            insurance=has_properties(
                operation_id='568142',
            ),
        ),
        has_properties(
            insurance=has_properties(
                operation_id='568143',
            ),
        ),
    ))


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_checkout_fail(httpretty):
    mock_im(httpretty, IM_INSURANCE_CHECKOUT_METHOD, responses=IM_INSURANCE_CHECKOUT_RESPONSE_INVALID)
    order = _create_order()

    with pytest.raises(ImError):
        checkout(order)


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_checkout_incorrect(httpretty):
    mock_im(httpretty, IM_INSURANCE_CHECKOUT_METHOD, responses=IM_INSURANCE_CHECKOUT_RESPONSE)
    order = _create_order()
    order.passengers[1].insurance = None

    with pytest.raises(InsuranceCheckoutException):
        checkout(order)
