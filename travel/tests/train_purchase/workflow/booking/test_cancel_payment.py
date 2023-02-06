# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.booking import cancel_payment
from travel.rasp.train_api.train_purchase.workflow.booking.cancel_payment import CancelPayment, CancelPaymentEvents
from travel.rasp.train_api.train_purchase.workflow.user_events import PaymentUserEvents


def _process(order):
    return process_state_action(CancelPayment, (CancelPaymentEvents.OK,), order)


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_cancel_payment_ok():
    original_order = TrainOrderFactory()
    with mock.patch.object(cancel_payment, 'send_event_to_payment', autospec=True) as m_send_event_to_payment:
        event, order = _process(original_order)

    assert event == CancelPaymentEvents.OK
    m_send_event_to_payment.assert_called_once_with(original_order.current_billing_payment,
                                                    PaymentUserEvents.ABORT_PAYMENT)
