# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

import common
from common.data_api.billing.trust_client import TrustClientRequestError, TrustPaymentStatuses
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.payment import cancel_payment
from travel.rasp.train_api.train_purchase.workflow.payment.cancel_payment import CancelPayment, CancelPaymentEvents

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def _process(payment):
    return process_state_action(CancelPayment, (CancelPaymentEvents.OK,), payment)


@pytest.mark.parametrize('payment_status, call_expected', (
    (TrustPaymentStatuses.STARTED, False),
    (TrustPaymentStatuses.AUTHORIZED, True),
))
def test_cancel_order_with_payment(payment_status, call_expected):
    order = TrainOrderFactory(payments=[dict(purchase_token='token')])

    with mock.patch.object(cancel_payment, 'get_payment_status',
                           autospec=True, return_value=payment_status) as m_get_payment_status, \
            mock.patch.object(cancel_payment, 'unhold_payment', autospec=True) as m_unhold_payment:
        event, payment = _process(order.current_billing_payment)

    assert event == CancelPaymentEvents.OK
    m_get_payment_status.assert_called_once_with(order, order.current_billing_payment)
    if call_expected:
        m_unhold_payment.assert_called_once_with(order, order.current_billing_payment)
    else:
        assert not m_unhold_payment.called
    assert payment.status == TrustPaymentStatuses.CANCELED.value


@mock.patch.object(common.utils.try_hard, 'sleep')
def test_cancel_payment_retry_ok(_m_sleep):
    with mock.patch.object(cancel_payment, 'unhold_payment',
                           autospec=True, side_effect=(TrustClientRequestError('connection error'), None)):
        original_order = TrainOrderFactory(
            payments=[dict(purchase_token='some_purchase_token')])
        event, payment = _process(original_order.current_billing_payment)

    assert event == CancelPaymentEvents.OK


def test_cancel_payment_not_done():
    order = TrainOrderFactory(payments=[dict(purchase_token=None)])

    with mock.patch.object(cancel_payment, 'unhold_payment', autospec=True) as m_unhold_payment:
        event, payment = _process(order.current_billing_payment)
        assert m_unhold_payment.call_count == 0

    assert event == CancelPaymentEvents.OK
