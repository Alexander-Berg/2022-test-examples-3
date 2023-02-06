# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest
from django.test import Client
from hamcrest import assert_that, has_entries, not_, contains
from rest_framework import status

from common.workflow import registry
from common.workflow.registry import get_process
from common.workflow.tests_utils.process import acquire_lock
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import PartnerDataFactory
from travel.rasp.train_api.train_purchase.views.test_utils import create_order
from travel.rasp.train_api.train_purchase.workflow.booking import TrainBookingUserEvents, TRAIN_BOOKING_PROCESS

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


def _create_order(status, cancelled=False):
    factory_args = {
        'status': status,
        'partner_data': PartnerDataFactory(is_order_cancelled=cancelled),
        'payments': [dict(purchase_token='purchase_token',
                          payment_url='payment_url',
                          create_payment_counter=1,
                          trust_created_at=datetime(2010, 1, 1))],
    }
    order = create_order(**factory_args)

    process = get_process(TRAIN_BOOKING_PROCESS, order)
    assert acquire_lock(process)
    process.init_process()
    order.update(set__travel_status=order.get_actual_travel_status())
    order.reload()

    return order


@pytest.yield_fixture
def m_run_process():
    with mock.patch.object(registry, 'run_process') as m_run_process:
        yield m_run_process


@pytest.mark.parametrize('order_status, result_status', [
    (OrderStatus.PAYMENT_FAILED, 200),
    (OrderStatus.DONE, status.HTTP_409_CONFLICT),
    (OrderStatus.RESERVED, status.HTTP_409_CONFLICT),
    (OrderStatus.CANCELLED, status.HTTP_409_CONFLICT),
])
def test_refund_process(m_run_process, order_status, result_status):
    ufs_order = _create_order(order_status)
    response = Client().post('/ru/api/train-purchase/orders/{}/retry_payment/'.format(ufs_order.uid))

    ufs_order.reload()

    assert response.status_code == result_status
    if response.status_code != 200:
        assert not m_run_process.apply_async.called
        assert_that(ufs_order.process,
                    not_(has_entries(external_events=contains(has_entries(name=TrainBookingUserEvents.RETRY_PAYMENT)))))
    else:
        m_run_process.apply_async.assert_called_once_with([TRAIN_BOOKING_PROCESS, str(ufs_order.id),
                                                           {'order_uid': ufs_order.uid}])
        assert_that(ufs_order.process,
                    has_entries(external_events=contains(has_entries(name=TrainBookingUserEvents.RETRY_PAYMENT))))

        assert ufs_order.status == OrderStatus.RESERVED
        assert ufs_order.travel_status == TravelOrderStatus.RESERVED
        assert len(ufs_order.payments) == 2, 'Должна быть история платежей'
        assert ufs_order.current_billing_payment.clear_at is None
        assert ufs_order.current_billing_payment.purchase_token is None
        assert ufs_order.current_billing_payment.payment_url is None
        assert ufs_order.current_billing_payment.resp_code is None
        assert ufs_order.current_billing_payment.resp_desc is None
        assert ufs_order.current_billing_payment.status is None
        assert not ufs_order.current_billing_payment.create_payment_counter


def test_order_cancelled(m_run_process):
    ufs_order = _create_order(OrderStatus.PAYMENT_FAILED, cancelled=True)
    response = Client().post('/ru/api/train-purchase/orders/{}/retry_payment/'.format(ufs_order.uid))

    assert response.status_code == status.HTTP_406_NOT_ACCEPTABLE
    assert not m_run_process.apply_async.called
