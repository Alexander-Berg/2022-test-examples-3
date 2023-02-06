# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from decimal import Decimal

import mock
import pytest

import travel.rasp.train_api.train_purchase.workflow.payment.create_refund_payment
from common.data_api.billing.trust_client import (
    TrustClient, TrustClientInvalidStatus, TrustRefundOrder
)
from common.tester.utils.datetime import replace_now
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, TicketFactory, TicketPaymentFactory,
    TicketRefundFactory, TrainRefundFactory, RefundPaymentFactory, InsuranceFactory,
)
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus
from travel.rasp.train_api.train_purchase.workflow.payment.create_refund_payment import (
    CreateRefundPayment, CreateRefundPaymentEvents, create_refund_payment
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _refund_payment_process(refund):
    action = {'action': CreateRefundPayment, 'kwargs': {'event_params': refund.id}}
    events = (CreateRefundPaymentEvents.CREATED, CreateRefundPaymentEvents.FAILED)
    return process_state_action(action, events, refund.payment)


@pytest.fixture
def m_trust_client():
    with mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.create_refund_payment,
                           'TrustClient', autospec=True) as m_trust_client:
        yield m_trust_client


@replace_now('2000-01-01 12:00:00')
def test_create_refund_payment_ok(m_trust_client):
    m_trust_client.return_value.create_refund.return_value = 'some-trust-refund-id'
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True}
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == CreateRefundPaymentEvents.CREATED
    assert refund.trust_refund_id == 'some-trust-refund-id'
    assert refund.refund_payment_status == RefundPaymentStatus.CREATED


@replace_now('2000-01-01 12:00:00')
def test_payment_not_cleared(m_trust_client):
    m_trust_client.return_value.is_payment_cleared.return_value = False
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True}
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == CreateRefundPaymentEvents.FAILED
    assert refund.refund_payment_status == RefundPaymentStatus.NEW


@replace_now('2000-01-01 12:00:00')
def test_create_refund_failed(m_trust_client):
    m_trust_client.return_value.create_refund.side_effect = TrustClientInvalidStatus('create refund error')
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True}
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == CreateRefundPaymentEvents.FAILED
    assert refund.refund_payment_finished_at == datetime(2000, 1, 1, 9)
    assert refund.refund_payment_status == RefundPaymentStatus.FAILED


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_1_blank_for_2_passengers(m_trust_client):
    m_trust_create_refund = m_trust_client.return_value.create_refund
    m_trust_create_refund.return_value = 'some-trust-refund-id'
    purchase_token = 'some-purchase-token123'
    original_order = TrainOrderFactory(
        payments=[dict(purchase_token=purchase_token)],
        passengers=[
            PassengerFactory(tickets=[TicketFactory(
                blank_id='1',
                payment=TicketPaymentFactory(ticket_order_id='33'),
                refund=TicketRefundFactory(amount=10, service_vat=None)
            )]),
            PassengerFactory(tickets=[TicketFactory(
                blank_id='1',
                payment=TicketPaymentFactory(amount=0),
                refund=TicketRefundFactory(amount=0)
            )])
        ]
    )
    train_refund = TrainRefundFactory(blank_ids=['1'], order_uid=original_order.uid)
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW,
        refund_blank_ids=train_refund.blank_ids,
        purchase_token=purchase_token,
        refund_uuid=train_refund.uuid
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == CreateRefundPaymentEvents.CREATED
    assert refund.trust_refund_id == 'some-trust-refund-id'
    assert refund.refund_payment_status == RefundPaymentStatus.CREATED
    m_trust_create_refund.assert_called_once_with([TrustRefundOrder(order_id=u'33', delta_amount=Decimal('10'))],
                                                  purchase_token)


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_blank_without_price(m_trust_client):
    m_trust_create_refund = m_trust_client.return_value.create_refund
    original_order = TrainOrderFactory(
        passengers=[
            PassengerFactory(tickets=[TicketFactory(
                blank_id='1',
                payment=TicketPaymentFactory(ticket_order_id='33'),
            )]),
            PassengerFactory(tickets=[TicketFactory(
                blank_id='2',
                payment=TicketPaymentFactory(amount=0),
                refund=TicketRefundFactory(amount=0)
            )])
        ]
    )
    train_refund = TrainRefundFactory(blank_ids=['2'], order_uid=original_order.uid)
    refund = RefundPaymentFactory(
        trust_refund_id=None,
        refund_payment_status=RefundPaymentStatus.NEW,
        refund_blank_ids=train_refund.blank_ids,
        purchase_token=original_order.current_billing_payment.purchase_token,
        refund_uuid=train_refund.uuid
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == CreateRefundPaymentEvents.CREATED
    assert refund.trust_refund_id is None
    assert refund.refund_payment_status == RefundPaymentStatus.DONE
    assert not m_trust_create_refund.called
    assert not m_trust_client.called


def test_create_refund_payment():
    trust_client = TrustClient()
    order = TrainOrderFactory(
        payments=[dict(purchase_token='some-purchase-token')],
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(
                    blank_id='1',
                    payment=TicketPaymentFactory(ticket_order_id='ticket_order_id_1',
                                                 service_order_id='service_order_id_1'),
                    refund=TicketRefundFactory(amount=0)
                )],
                insurance=InsuranceFactory(trust_order_id='insurance_order_id_1', operation_id='ins_op_1'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(
                    blank_id='2',
                    payment=TicketPaymentFactory(ticket_order_id='ticket_order_id_2',
                                                 service_order_id='service_order_id_2'),
                    refund=TicketRefundFactory(amount=10)
                )],
                insurance=InsuranceFactory(trust_order_id='insurance_order_id_2', amount=Decimal(50),
                                           operation_id='ins_op_2'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(
                    blank_id='3',
                    payment=TicketPaymentFactory(ticket_order_id='ticket_order_id_3',
                                                 service_order_id='service_order_id_3'),
                    refund=TicketRefundFactory(amount=100, service_vat=None)
                )],
                insurance=InsuranceFactory(trust_order_id='insurance_order_id_3', amount=Decimal(50),
                                           operation_id='ins_op_3'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(
                    blank_id='4',
                    payment=TicketPaymentFactory(ticket_order_id='ticket_order_id_4',
                                                 service_order_id='service_order_id_4',
                                                 service_amount=Decimal('55.00')),
                    refund=TicketRefundFactory(amount=100, service_vat=dict(rate=0, amount=0))
                )],
                insurance=InsuranceFactory(trust_order_id='insurance_order_id_4', amount=Decimal(150),
                                           operation_id='ins_op_4'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(
                    blank_id='5',
                    payment=TicketPaymentFactory(ticket_order_id='ticket_order_id_5',
                                                 service_order_id='service_order_id_5',
                                                 service_amount=Decimal('55.00'),
                                                 service_vat=dict(rate=0, amount=0)),
                    refund=TicketRefundFactory(amount=100, service_vat=None)
                )],
                insurance=InsuranceFactory(trust_order_id='insurance_order_id_5', amount=Decimal(100),
                                           operation_id='ins_op_5'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(
                    blank_id='1',
                    refund=TicketRefundFactory(amount=0),
                )],
                insurance=InsuranceFactory(trust_order_id='insurance_order_id_6', amount=Decimal(70),
                                           operation_id='ins_op_6'),
            ),
        ]
    )
    train_refund = TrainRefundFactory(blank_ids=['1', '3', '4', '5'], order_uid=order.uid)
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW,
        refund_blank_ids=train_refund.blank_ids,
        purchase_token=order.current_billing_payment.purchase_token,
        refund_uuid=train_refund.uuid,
        refund_insurance_ids=['ins_op_1', 'ins_op_3', 'ins_op_4', 'ins_op_5', 'ins_op_6'],
    )

    with mock.patch.object(trust_client, 'create_refund',
                           autospec=True, return_value='trust_refund_id') as m_create_refund:
        assert create_refund_payment(trust_client, order, refund) == 'trust_refund_id'
        m_create_refund.assert_called_once_with(
            [
                TrustRefundOrder(order_id='ticket_order_id_3', delta_amount=Decimal(100)),
                TrustRefundOrder(order_id='service_order_id_4', delta_amount=Decimal(55)),
                TrustRefundOrder(order_id='ticket_order_id_4', delta_amount=Decimal(45)),
                TrustRefundOrder(order_id='service_order_id_5', delta_amount=Decimal(55)),
                TrustRefundOrder(order_id='ticket_order_id_5', delta_amount=Decimal(45)),
                TrustRefundOrder(order_id='insurance_order_id_1', delta_amount=Decimal(100)),
                TrustRefundOrder(order_id='insurance_order_id_3', delta_amount=Decimal(50)),
                TrustRefundOrder(order_id='insurance_order_id_4', delta_amount=Decimal(150)),
                TrustRefundOrder(order_id='insurance_order_id_5', delta_amount=Decimal(100)),
                TrustRefundOrder(order_id='insurance_order_id_6', delta_amount=Decimal(70)),
            ],
            'some-purchase-token'
        )


def test_amount_less_than_service():
    trust_client = TrustClient()
    order = TrainOrderFactory(
        payments=[dict(purchase_token='some-purchase-token')],
        passengers=[
            PassengerFactory(tickets=[TicketFactory(
                blank_id='1',
                payment=TicketPaymentFactory(
                    amount=300,
                    ticket_order_id='ticket_order_id_1',
                    service_order_id='service_order_id_1',
                    service_amount=Decimal('55.00')
                ),
                refund=TicketRefundFactory(amount=30)
            )]),
        ]
    )
    train_refund = TrainRefundFactory(blank_ids=['1'], order_uid=order.uid)
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW,
        refund_blank_ids=train_refund.blank_ids,
        purchase_token=order.current_billing_payment.purchase_token,
        refund_uuid=train_refund.uuid
    )

    with mock.patch.object(trust_client, 'create_refund',
                           autospec=True, return_value='trust_refund_id') as m_create_refund:
        assert create_refund_payment(trust_client, order, refund) == 'trust_refund_id'
        m_create_refund.assert_called_once_with(
            [
                TrustRefundOrder(order_id='service_order_id_1', delta_amount=Decimal(30)),
            ],
            'some-purchase-token'
        )


@replace_now('2000-01-01 12:00:00')
def test_create_refund_payment_status_unknown(m_trust_client):
    m_trust_client.return_value.create_refund.return_value = 'some-trust-refund-id'
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.UNKNOWN, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True}
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == CreateRefundPaymentEvents.FAILED
    assert refund.trust_refund_id == 'some-trust-refund-id'
    assert refund.refund_payment_status == RefundPaymentStatus.UNKNOWN


def test_create_refund_payment_with_yandex_fee():
    trust_client = TrustClient()
    order = TrainOrderFactory(
        payments=[dict(purchase_token='some-purchase-token')],
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(
                    blank_id='1',
                    payment=TicketPaymentFactory(
                        ticket_order_id='ticket_order_id_1',
                        fee_order_id='fee_order_id_1',
                    ),
                    refund=TicketRefundFactory(
                        amount=Decimal(100),
                        refund_yandex_fee_amount=Decimal(50),
                    )
                )],
            ),
        ]
    )
    train_refund = TrainRefundFactory(blank_ids=['1'], order_uid=order.uid)
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW,
        refund_blank_ids=train_refund.blank_ids,
        purchase_token=order.current_billing_payment.purchase_token,
    )

    with mock.patch.object(trust_client, 'create_refund',
                           autospec=True, return_value='trust_refund_id') as m_create_refund:
        assert create_refund_payment(trust_client, order, refund) == 'trust_refund_id'
        m_create_refund.assert_called_once_with(
            [
                TrustRefundOrder(order_id='ticket_order_id_1', delta_amount=Decimal(100)),
                TrustRefundOrder(order_id='fee_order_id_1', delta_amount=Decimal(50)),
            ],
            'some-purchase-token'
        )
