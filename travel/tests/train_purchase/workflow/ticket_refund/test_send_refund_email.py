# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, TrainRefundFactory
from travel.rasp.train_api.train_purchase.core.models import RefundStatus
from travel.rasp.train_api.train_purchase.workflow.ticket_refund import send_refund_email
from travel.rasp.train_api.train_purchase.workflow.ticket_refund.send_refund_email import SendRefundEmail, SendRefundEmailEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _process(refund):
    return process_state_action(SendRefundEmail, (SendRefundEmailEvents.DONE,), refund)


@mock.patch.object(send_refund_email, 'send_refund_email')
def test_ok(m_send_refund_email):
    order = TrainOrderFactory()
    refund = TrainRefundFactory(order_uid=order.uid)

    event, refund = _process(refund)
    order.reload()

    assert event == SendRefundEmailEvents.DONE
    assert refund.status == RefundStatus.DONE
    assert refund.email_is_sent

    m_send_refund_email.assert_called_once_with(refund)


@mock.patch.object(send_refund_email, 'send_refund_email')
def test_not_need_to_send_email(m_send_refund_email):
    order = TrainOrderFactory()
    refund = TrainRefundFactory(order_uid=order.uid, email_is_sent=True)

    event, refund = _process(refund)
    order.reload()

    assert event == SendRefundEmailEvents.DONE
    assert refund.status == RefundStatus.DONE
    assert not m_send_refund_email.called
