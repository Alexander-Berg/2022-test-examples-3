# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from travel.rasp.train_api.train_purchase.core.factories import RefundPaymentFactory
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus
from travel.rasp.train_api.train_purchase.tasks.finish_refund_payment import retry
from travel.rasp.train_api.train_purchase.tasks.finish_refund_payment.retry import retry_refund_payment
from travel.rasp.train_api.train_purchase.workflow.user_events import PaymentUserEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_retry_refund_payment():
    refund_payment = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.NEW,
        is_email_sent=False,
        trust_refund_id=None,
        factory_extra_params={'create_order': True},
    )
    with mock.patch.object(retry, 'send_event_to_payment', autospec=True) as m_send_event_to_payment:

        retry_refund_payment()
        m_send_event_to_payment.assert_called_once_with(
            refund_payment.payment, PaymentUserEvents.REFUND_PAYMENT, params=refund_payment.id
        )
