# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import PartnerDataFactory, TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.booking import create_payment
from travel.rasp.train_api.train_purchase.workflow.booking.create_payment import CreatePayment, CreatePaymentEvents
from travel.rasp.train_api.train_purchase.workflow.payment import PAYMENT_PROCESS

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


@pytest.yield_fixture
def m_run_process():
    with mock.patch.object(create_payment, 'run_process') as m_run_process:
        yield m_run_process


def _process(order):
    return process_state_action(
        CreatePayment,
        (CreatePaymentEvents.OK, CreatePaymentEvents.ORDER_CANCELLED, CreatePaymentEvents.PAYMENT_ALREADY_STARTED),
        order,
    )


def test_create_payment_ok(m_run_process):
    original_order = TrainOrderFactory()
    event, order = _process(original_order)

    m_run_process.apply_async.assert_called_once_with([
        PAYMENT_PROCESS,
        str(order.current_billing_payment.id),
        {'order_uid': original_order.uid, 'payment': original_order.current_billing_payment.uid}
    ])
    assert event == CreatePaymentEvents.OK


def test_create_payment_cancelled(m_run_process):
    original_order = TrainOrderFactory(partner_data=PartnerDataFactory(is_order_cancelled=True))
    event, order = _process(original_order)

    assert not m_run_process.apply_async.called
    assert event == CreatePaymentEvents.ORDER_CANCELLED


def test_create_payment_already_started(m_run_process):
    original_order = TrainOrderFactory(payments=[{'purchase_token': 'someToken'}])
    event, order = _process(original_order)

    assert not m_run_process.apply_async.called
    assert event == CreatePaymentEvents.PAYMENT_ALREADY_STARTED
