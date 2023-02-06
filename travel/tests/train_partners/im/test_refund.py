# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, has_entry, has_entries, contains

from travel.rasp.train_api.train_partners.im.base import log
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.refund import REFUND_ENDPOINT, make_refund
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory

REFUND_RESPONSE = """{
    "ServiceReturnResponse": {
        "$type": "ApiContracts.Railway.V1.Messages.Return.RailwayAutoReturnResponse, ApiContracts",
        "Blanks": [{
            "PurchaseOrderItemBlankId": 75198,
            "ReturnOrderItemBlankId": 75199,
            "Amount": 1127.6,
            "VatRateValues": [
                {"Rate":0.0,"Value":0.00},
                {"Rate":18.0,"Value":21.08},
                {"Rate":18.0,"Value":0.0},
                {"Rate":18.0,"Value":28.28}
            ]
        }],
        "Amount": 1127.6,
        "Fare": 1313.0,
        "Tax": 0.0,
        "Confirmed": "2017-07-31T22:45:03",
        "ReturnOrderItemId": 79126,
        "AgentReferenceId": "100500",
        "ClientFeeCalculation":null,
        "AgentFeeCalculation": {"Charge": 33.87, "Profit": 0.0}
    }
}"""


@mock.patch.object(log, 'info')
@pytest.mark.dbuser
@pytest.mark.mongouser
@pytest.mark.parametrize('blank_id', ('75198', None))
def test_parsing(m_log, httpretty, blank_id):
    order = TrainOrderFactory(partner_data_history=[PartnerDataFactory(operation_id='3040')])
    mock_im(httpretty, REFUND_ENDPOINT, body=REFUND_RESPONSE)
    result = make_refund(order=order, blank_id=blank_id, doc_id='doc_id', reference_id='reference_id')

    assert len(result.refund_by_blank_id) == 1
    assert result.refund_by_blank_id['75198'].amount == Decimal('1127.6')
    assert result.refund_by_blank_id['75198'].refund_operation_id == '79126'
    assert_that(httpretty.last_request.parsed_body, has_entry('ServiceAutoReturnRequest', has_entries({
        'CheckDocumentNumber': 'doc_id',
        'OrderItemId': 3040,
        'OrderItemBlankIds': contains(int(blank_id)) if blank_id else None,
        'AgentReferenceId': 'reference_id',
    })))
    assert '"CheckDocumentNumber": "******"' in m_log.call_args_list[0][0][2]
