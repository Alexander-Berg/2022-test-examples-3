# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import sys
from datetime import datetime
from decimal import Decimal
from io import BytesIO

import pytest
from hamcrest import assert_that, contains

from travel.rasp.train_api.scripts.export_orders_and_refunds import export_orders_and_refunds, _create_parser
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PartnerDataFactory, PaymentFactory, PassengerFactory, TicketFactory, InsuranceFactory,
    TrainRefundFactory, RefundPaymentFactory
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_export_orders_and_refunds():
    def _get_result(stream):
        result = []
        stream.seek(0)
        while True:
            line = stream.readline().rstrip()
            if not line:
                break
            result.append([s if s else None for s in line.split(',')])
        return result

    actual_order = TrainOrderFactory(
        finished_at=datetime(2019, 5, 5, 13, 5),
        partner_data_history=[PartnerDataFactory(order_num='orderNum', im_order_id=555)],
        payments=[PaymentFactory(
            purchase_token='purchaseToken',
            trust_created_at=datetime(2019, 5, 5, 13, 0),
            clear_at=datetime(2019, 5, 6, 6, 0),
        )],
        passengers=[
            PassengerFactory(
                insurance=InsuranceFactory(trust_order_id='purchased1', operation_id='i1', amount=Decimal(60)),
                tickets=[TicketFactory(blank_id='1')],
            ),
            PassengerFactory(
                tickets=[TicketFactory(blank_id='2')],
            ),
            PassengerFactory(
                insurance=InsuranceFactory(trust_order_id='purchased3', operation_id='i3', amount=Decimal(60)),
                tickets=[TicketFactory(blank_id='3')],
            ),
        ],
    )
    TrainOrderFactory(finished_at=datetime(2019, 4, 5, 13, 0))
    actual_order_without_history_and_payments = TrainOrderFactory(
        finished_at=datetime(2019, 5, 7, 13, 0),
        partner_data_history=[],
        payments=[],
    )
    actual_refund = TrainRefundFactory(
        created_at=datetime(2019, 5, 5, 14, 5),
        blank_ids=['1', '2'],
        insurance_ids=['i1'],
        order_uid=actual_order.uid,
    )
    another_actual_refund = TrainRefundFactory(
        created_at=datetime(2019, 5, 6, 14, 5),
        blank_ids=['3'],
        insurance_ids=['i3'],
        order_uid=actual_order.uid,
    )
    TrainRefundFactory(
        created_at=datetime(2019, 5, 7, 14, 5),
        order_uid=actual_order_without_history_and_payments.uid,
    )
    RefundPaymentFactory(
        purchase_token='purchaseToken',
        trust_refund_id='someTrustRefundId',
        refund_uuid=actual_refund.uuid,
        refund_created_at=datetime(2019, 5, 5, 14, 0),
    )
    RefundPaymentFactory(
        purchase_token='purchaseToken',
        trust_refund_id=None,
        trust_reversal_id='someReversalId',
        refund_uuid=another_actual_refund.uuid,
        refund_created_at=datetime(2019, 5, 6, 14, 0),
    )

    stream_for_orders = BytesIO()
    stream_for_refunds = BytesIO()

    export_orders_and_refunds(min_date=datetime(2019, 5, 1), max_date=datetime(2019, 6, 1),
                              stream_for_orders=stream_for_orders, stream_for_refunds=stream_for_refunds)

    assert_that(_get_result(stream_for_orders), contains(
        contains(
            'OrderId',
            'ImOrderId',
            'PurchaseToken',
            'DateTime',
            'TrustDateTime',
            'ClearDateTime',
            'Amount',
            'Insurance',
            'Fee',
        ),
        contains(
            'orderNum',
            '555',
            'purchaseToken',
            '2019-05-05 13:05:00',
            '2019-05-05 13:00:00',
            '2019-05-06 06:00:00',
            '300.00',
            '120.00',
            '210.00',
        ),
        contains(
            None,
            None,
            None,
            '2019-05-07 13:00:00',
            None,
            None,
            '100.00',
            '0',
            '70.00',
        ),
    ))

    assert_that(_get_result(stream_for_refunds), contains(
        contains(
            'OrderId',
            'ImOrderId',
            'PurchaseToken',
            'TrustRefundId',
            'TrustReversalId',
            'DateTime',
            'TrustDateTime',
            'Amount',
            'Insurance',
        ),
        contains(
            'orderNum',
            '555',
            'purchaseToken',
            'someTrustRefundId',
            None,
            '2019-05-05 14:05:00',
            '2019-05-05 14:00:00',
            '200.00',
            '60.00',
        ),
        contains(
            'orderNum',
            '555',
            'purchaseToken',
            None,
            'someReversalId',
            '2019-05-06 14:05:00',
            '2019-05-06 14:00:00',
            '100.00',
            '60.00',
        ),
        contains(
            None,
            None,
            None,
            None,
            None,
            '2019-05-07 14:05:00',
            None,
            '0',
            '0',
        ),
    ))

    stream_for_orders.close()
    stream_for_refunds.close()


def test_parser():
    parser = _create_parser()
    args = parser.parse_args(['--min_date', '2019-05-01', '--max_date', '2019-06-01'])

    assert args.min_date == datetime(2019, 5, 1)
    assert args.max_date == datetime(2019, 6, 1)
    assert isinstance(args.orders_out, type(sys.stdout))
    assert isinstance(args.refunds_out, type(sys.stdout))
