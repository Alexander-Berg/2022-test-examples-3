# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.workflow import registry
from travel.rasp.train_api.train_purchase.core.factories import PaymentFactory, TrainOrderFactory, TrainRefundFactory
from travel.rasp.train_api.train_purchase.tasks.revivers import (
    revive_train_booking_processes, revive_train_payment_processes, REVIVE_FREQUENCY,
    revive_ticket_refund_processes
)


@pytest.mark.mongouser
@pytest.mark.dbuser
@mock.patch.object(registry.run_process, 'apply_async')
def test_revive_train_booking(m_run_process_apply_async):
    from travel.rasp.train_api.train_purchase.workflow.booking import OrderState, TRAIN_BOOKING_PROCESS

    order = TrainOrderFactory(process={'state': OrderState.CONFIRMING})
    revive_train_booking_processes()

    m_run_process_apply_async.assert_called_once_with([TRAIN_BOOKING_PROCESS, str(order.id), {'order_uid': order.uid}],
                                                      expiration=REVIVE_FREQUENCY)


@pytest.mark.mongouser
@pytest.mark.dbuser
@mock.patch.object(registry.run_process, 'apply_async')
def test_revive_train_payment(m_run_process_apply_async):
    from travel.rasp.train_api.train_purchase.workflow.payment import PAYMENT_PROCESS, PaymentState

    payment = PaymentFactory(process={'state': PaymentState.PAYING})
    revive_train_payment_processes()

    m_run_process_apply_async.assert_called_once_with([PAYMENT_PROCESS, str(payment.id),
                                                       {'order_uid': payment.order_uid}],
                                                      expiration=REVIVE_FREQUENCY)


@pytest.mark.mongouser
@pytest.mark.dbuser
@mock.patch.object(registry.run_process, 'apply_async')
def test_revive_ticket_refund(m_run_process_apply_async):
    from travel.rasp.train_api.train_purchase.workflow.ticket_refund import TICKET_REFUND_PROCESS, RefundState

    refund = TrainRefundFactory(process={'state': RefundState.CREATE_REFUND_PAYMENT})
    revive_ticket_refund_processes()

    m_run_process_apply_async.assert_called_once_with(
        [TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}], expiration=REVIVE_FREQUENCY
    )
