# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

import travel.rasp.train_api.train_purchase.workflow.payment.clear_payment
from common.data_api.billing.trust_client import TrustClientRequestError, TrustPaymentInfo, TrustPaymentStatuses
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, RefundPaymentFactory
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus
from travel.rasp.train_api.train_purchase.workflow.payment import ClearPaymentEvents, ClearPayment

pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


def _process(payment):
    return process_state_action(ClearPayment, (ClearPaymentEvents.OK,), payment)


@mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.clear_payment.TrustClient, 'clear_payment', autospec=True)
def test_clear_payment_ok(m_clear_payment):
    side_effects = [TrustClientRequestError('a_few_of_request_errors #%d' % i) for i in range(2)]
    side_effects.append(None)
    m_clear_payment.side_effect = side_effects

    original_order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                       payments=[dict(purchase_token='some_purchase_token')])
    event, payment = _process(original_order.current_billing_payment)

    assert m_clear_payment.call_count == len(side_effects)
    assert event == ClearPaymentEvents.OK


@mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.clear_payment.TrustClient, 'clear_payment', autospec=True)
def test_clear_payment_retries_exceeded(m_clear_payment):
    side_effects = [TrustClientRequestError('a_lot_of_request_errors #%d' % i) for i in range(5)]
    m_clear_payment.side_effect = side_effects

    original_order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                       payments=[dict(purchase_token='some_purchase_token')])
    event, payment = _process(original_order.current_billing_payment)

    assert m_clear_payment.call_count == len(side_effects)
    assert event != ClearPaymentEvents.OK


@mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.clear_payment.TrustClient, 'clear_payment', autospec=True)
def test_clear_payment_fail(m_clear_payment):
    m_clear_payment.side_effect = [Exception('another_error')]

    original_order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                       payments=[dict(purchase_token='some_purchase_token')])
    event, payment = _process(original_order.current_billing_payment)

    assert m_clear_payment.call_count == 1
    assert event != ClearPaymentEvents.OK


@mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.clear_payment, 'TrustClient', autospec=True)
def test_clear_payment_no_need_to_clear(m_trust_client):
    original_order = TrainOrderFactory(
        status=OrderStatus.RESERVED,
        payments=[{'purchase_token': 'some_purchase_token', 'immediate_return': True}]
    )
    event, payment = _process(original_order.current_billing_payment)
    assert not m_trust_client.called
    assert event == ClearPaymentEvents.OK


@mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.clear_payment, 'TrustClient', autospec=True)
def test_clear_after_resize(m_trust_client):
    m_trust_client.return_value.get_payment_info.return_value = TrustPaymentInfo({
        'payment_status': TrustPaymentStatuses.CLEARED,
        'reversal_id': '123123123'
    })
    purchase_token = 'some_purchase_token'

    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True}, purchase_token=purchase_token, payment_resized=True
    )
    payment = refund_payment.payment
    payment.current_refund_payment_id = refund_payment.id
    payment.save()
    event, payment = _process(payment)
    refund_payment.reload()

    m_trust_client.return_value.clear_payment.assert_called_once_with(purchase_token)
    m_trust_client.return_value.get_payment_info.assert_called_once_with(purchase_token)
    assert event == ClearPaymentEvents.OK
    assert refund_payment.refund_payment_status == RefundPaymentStatus.DONE
    assert refund_payment.trust_reversal_id == '123123123'


@mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.clear_payment, 'TrustClient', autospec=True)
def test_skip_cleared(m_trust_client):
    original_order = TrainOrderFactory(
        status=OrderStatus.RESERVED,
        payments=[dict(
            purchase_token='some_purchase_token',
            clear_at=datetime(2018, 10, 9)
        )]
    )
    event, payment = _process(original_order.current_billing_payment)

    assert not m_trust_client.called
    assert event == ClearPaymentEvents.OK
