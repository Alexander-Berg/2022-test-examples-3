# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest
from hamcrest import assert_that, contains, has_properties

from travel.rasp.train_api.train_partners.base.insurance.pricing import pricing
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.insurance.pricing import IM_INSURANCE_PRICING_METHOD
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory, PassengerFactory
from travel.rasp.train_api.train_purchase.core.models import TrainPartner


IM_INSURANCE_PRICING_RESPONSE = """
{"PricingResult": {"$type": "ApiContracts.Insurance.V1.Messages.Travel.RailwayTravelPricingResult, ApiContracts",
  "ProductPricingInfoList": [{"$type": "ApiContracts.Insurance.V1.Messages.Travel.RailwayTravelProductPricingInfo...",
    "Amount": 100.0,
    "Company": "Renessans",
    "Compensation": 3000.0,
    "OrderCustomerId": 831463,
    "OrderItemId": 568141,
    "Package": "AccidentWithFloatPremium",
    "Provider": "P3"},
   {"$type": "ApiContracts.Insurance.V1.Messages.Travel.RailwayTravelProductPricingInfo, ApiContracts",
    "Amount": 70.00,
    "Company": "Renessans",
    "Compensation": 300.0,
    "OrderCustomerId": 831464,
    "OrderItemId": 568141,
    "Package": "AccidentWithFloatPremium",
    "Provider": "P3"}]}}
"""


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_im_pricing(httpretty):
    mock_im(httpretty, IM_INSURANCE_PRICING_METHOD, body=IM_INSURANCE_PRICING_RESPONSE)
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(operation_id='568141'),
        passengers=[PassengerFactory(customer_id='831463'), PassengerFactory(customer_id='831464')],
    )

    pricing(order)
    order.reload()

    assert_that(httpretty.last_request, has_properties(
        parsed_body={
            'Product': {
                '$type': 'ApiContracts.Insurance.V1.Messages.Travel.RailwayTravelPricingRequest, ApiContracts',
                'MainServiceReference': {
                    '$type': 'ApiContracts.Insurance.V1.Messages.Travel.MainServiceReferenceInternal, ApiContracts',
                    'OrderItemId': 568141,
                }
            }
        }
    ))
    assert_that(order.passengers, contains(
        has_properties(
            customer_id='831463',
            insurance=has_properties(
                amount=Decimal('100.0'),
                company='Renessans',
                package='AccidentWithFloatPremium',
                provider='P3',
                compensation=Decimal('3000.0'),
                compensation_variants=[]
            ),
        ),
        has_properties(
            customer_id='831464',
            insurance=has_properties(
                amount=Decimal('70.0'),
                company='Renessans',
                package='AccidentWithFloatPremium',
                provider='P3',
                compensation=Decimal('300.0'),
                compensation_variants=[]
            ),
        ),
    ))
