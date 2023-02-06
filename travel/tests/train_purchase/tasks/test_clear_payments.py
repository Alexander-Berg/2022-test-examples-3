# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

from common.data_api.billing.trust_client import TrustPaymentStatuses
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_purchase.core.factories import PaymentFactory
from travel.rasp.train_api.train_purchase.tasks import start_clear_payments, clear_payments
from travel.rasp.train_api.train_purchase.workflow.user_events import PaymentUserEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@replace_now('2018-10-09')
@replace_dynamic_setting('TRAIN_PURCHASE_PAYMENT_CLEAR_DELAY_HOURS', 47)
def test_start_clear_payments():
    payment_need_clear = PaymentFactory(
        status=TrustPaymentStatuses.AUTHORIZED.value,
        trust_created_at=datetime(2018, 10, 6, 21),
        clear_at=None,
    )
    PaymentFactory(
        status=TrustPaymentStatuses.AUTHORIZED.value,
        trust_created_at=datetime(2018, 10, 7, 2),
        clear_at=None,
    )
    PaymentFactory(
        status=TrustPaymentStatuses.CLEARED.value,
        trust_created_at=datetime(2018, 10, 6, 21),
        clear_at=datetime(2018, 10, 8, 21, 10),
    )
    PaymentFactory(
        status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
        trust_created_at=datetime(2018, 10, 6, 21),
        clear_at=None,
    )
    with mock.patch.object(clear_payments, 'send_event_to_payment', autospec=True) as m_send_event_to_payment:
        start_clear_payments()
        m_send_event_to_payment.assert_called_once_with(
            payment_need_clear, PaymentUserEvents.CONFIRM_PAYMENT,
        )
