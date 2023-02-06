# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, has_properties, contains_inanyorder

import travel.rasp.train_api.train_purchase.workflow.payment.resize_payment
from common.data_api.billing.trust_client import TrustClientInvalidStatus
from common.tester.utils.datetime import replace_now
from common.utils import gen_hex_uuid
from travel.rasp.library.python.common23.date.environment import now_utc
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import (
    RefundPaymentFactory, TrainOrderFactory, TicketFactory,
    PassengerFactory, TicketPaymentFactory, TicketRefundFactory, TrainRefundFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus
from travel.rasp.train_api.train_purchase.workflow.payment import ResizePaymentEvents, ResizePayment

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _resize_payment_process(refund):
    events = (ResizePaymentEvents.OK,)
    action = {'action': ResizePayment, 'kwargs': {'event_params': refund.id}}
    return process_state_action(action, events, refund.payment)


@pytest.fixture
def m_trust_client():
    with mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.resize_payment,
                           'TrustClient', autospec=True) as m_trust_client:
        yield m_trust_client


def create_refund_payment(tickets, **kwargs):
    kwargs.setdefault('order_uid', gen_hex_uuid())
    kwargs.setdefault('refund_uuid', gen_hex_uuid())
    kwargs.setdefault('refund_payment_status', RefundPaymentStatus.NEW)
    kwargs.setdefault('refund_blank_ids', [t.blank_id for t in tickets if t.refund])
    kwargs.setdefault('trust_refund_id', 'some-trust-refund-id')
    kwargs.setdefault('refund_created_at', now_utc())
    kwargs.setdefault('refund_payment_finished_at', None)
    kwargs.setdefault('purchase_token', 'some-purchase-token')
    TrainOrderFactory(
        uid=kwargs['order_uid'],
        payments=[
            dict(purchase_token=kwargs['purchase_token'])
        ],
        passengers=[
            PassengerFactory(tickets=[t]) for t in tickets
        ],
    )
    TrainRefundFactory(
        is_active=True,
        order_uid=kwargs['order_uid'],
        uuid=kwargs['refund_uuid'],
        blank_ids=kwargs['refund_blank_ids'],
    )
    return RefundPaymentFactory(**kwargs)


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_ok(m_trust_client):
    refund = create_refund_payment(
        tickets=[
            TicketFactory(blank_id='1001', payment=TicketPaymentFactory(
                ticket_order_id='9991',
                amount=1100,
                service_order_id='9992',
                service_amount=22,
                fee_order_id='9993',
                fee=50,
            ), refund=TicketRefundFactory(
                amount=822,
                refund_yandex_fee_amount=Decimal('40'),
            ))
        ],
        trust_refund_id=None,
        purchase_token='some-purchase-token',
    )

    event, payment = _resize_payment_process(refund)
    refund.reload()

    assert event == ResizePaymentEvents.OK
    assert_that(refund, has_properties(
        trust_refund_id=None,
        payment_resized=True,
        refund_payment_finished_at=datetime(2000, 1, 1, 9),
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
    ))

    resize_payment_calls = m_trust_client.return_value.resize_payment_order.call_args_list
    assert_that(resize_payment_calls, contains_inanyorder(
        mock.call('some-purchase-token', '9993', Decimal(50 - 40)),
        mock.call('some-purchase-token', '9991', Decimal(1100 - 22 - (822 - 22))),
        mock.call('some-purchase-token', '9992', Decimal(0))
    ))


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_start_refund_failed(m_trust_client):
    m_trust_client.return_value.resize_payment_order.side_effect = TrustClientInvalidStatus('resize error')
    refund = create_refund_payment(tickets=[TicketFactory(refund=TicketRefundFactory())])

    event, payment = _resize_payment_process(refund)
    assert event == 'noevent.unhandled_exception_state'
