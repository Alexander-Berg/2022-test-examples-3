# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest
from hamcrest import assert_that, has_entry, has_entries, contains, has_properties

from travel.rasp.train_api.train_partners.base import Tax
from travel.rasp.train_api.train_partners.im import get_refund_amount
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.refund_amount import RETURN_AMOUNT_ENDPOINT
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory


response = """{
    "ServiceReturnResponse": {
        "$type": "ApiContracts.Railway.V1.Messages.Return.RailwayReturnAmountResponse, ApiContracts",
        "Blanks": [{
            "PurchaseOrderItemBlankId": 75049,
            "ReturnOrderItemBlankId": 0,
            "Amount": 1467.1,
            "VatRateValues": [
                {"Rate":1.1,"Value":11.11},
                {"Rate":2.2,"Value":22.22},
                {"Rate":3.3,"Value":33.33},
                {"Rate":4.4,"Value":44.44}
            ]
        }],
        "Amount": 1533.1
    }
}"""


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_refund_amount(httpretty):
    order = TrainOrderFactory(partner_data_history=[PartnerDataFactory(operation_id='1020')])
    mock_im(httpretty, RETURN_AMOUNT_ENDPOINT, body=response)
    blanks = get_refund_amount(order=order, doc_number='1234567890', blank_id='75049')

    assert_that(blanks, contains(has_properties(
        id='75049',
        amount=Decimal('1467.1'),
        tariff_vat=Tax(rate=Decimal('1.1'), amount=Decimal('11.11')),
        service_vat=Tax(rate=Decimal('2.2'), amount=Decimal('22.22')),
        commission_fee_vat=Tax(rate=Decimal('3.3'), amount=Decimal('33.33')),
        refund_commission_fee_vat=Tax(rate=Decimal('4.4'), amount=Decimal('44.44'))
    )))
    assert_that(httpretty.last_request.parsed_body, has_entry('ServiceReturnAmountRequest', has_entries({
        'OrderItemBlankIds': contains(75049),
        'OrderItemId': 1020,
        'CheckDocumentNumber': '1234567890'
    })))
