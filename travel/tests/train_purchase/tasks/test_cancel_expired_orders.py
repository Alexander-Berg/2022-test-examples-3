# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, timedelta

import mock
import pytest

from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory
from travel.rasp.train_api.train_purchase.tasks import find_and_cancel_expired_orders

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


@replace_now('2018-07-18 16:00:00')
def test_cancel_orders():
    reserved_to = MSK_TZ.localize(datetime(2018, 7, 18, 15, 55))
    statuses_must_be_checked = [
        OrderStatus.PAYMENT_FAILED, OrderStatus.START_PAYMENT_FAILED, OrderStatus.PAYMENT_OUTDATED,
        OrderStatus.CONFIRM_FAILED, OrderStatus.CANCELLED, OrderStatus.RESERVED
    ]
    orders = [TrainOrderFactory(reserved_to=reserved_to, status=s) for s in statuses_must_be_checked]
    with mock.patch('travel.rasp.train_api.train_purchase.tasks.cancel_expired_orders.cancel_order') as m_cancel_order:
        find_and_cancel_expired_orders(timedelta(minutes=10))
        assert m_cancel_order.call_count == len(statuses_must_be_checked)
    for order in orders:
        assert order.reload().current_partner_data.is_order_cancelled


@replace_now('2018-07-18 16:00:00')
def test_cancel_order_many_reservations():
    reserved_to = MSK_TZ.localize(datetime(2018, 7, 18, 15, 55))
    order = TrainOrderFactory(
        reserved_to=reserved_to,
        status=OrderStatus.CONFIRM_FAILED,
        partner_data_history=[
            PartnerDataFactory(im_order_id='000', is_order_cancelled=True),
            PartnerDataFactory(im_order_id='111'),
            PartnerDataFactory(im_order_id='222')]
    )
    with mock.patch('travel.rasp.train_api.train_purchase.tasks.cancel_expired_orders.cancel_order') as m_cancel_order:
        find_and_cancel_expired_orders(timedelta(minutes=10))
        assert m_cancel_order.call_count == 2
    assert order.reload().partner_data_history[0].is_order_cancelled
    assert order.reload().partner_data_history[1].is_order_cancelled
    assert order.reload().partner_data_history[2].is_order_cancelled


@replace_now('2018-07-18 16:00:00')
def test_reserved_to_not_in_time_range():
    order1 = TrainOrderFactory(
        reserved_to=MSK_TZ.localize(datetime(2018, 7, 18, 15, 45)), status=OrderStatus.PAYMENT_FAILED,
        partner_data=PartnerDataFactory(is_order_cancelled=False)
    )
    order2 = TrainOrderFactory(
        reserved_to=MSK_TZ.localize(datetime(2018, 7, 18, 16, 45)), status=OrderStatus.PAYMENT_FAILED,
        partner_data=PartnerDataFactory(is_order_cancelled=False)
    )
    order3 = TrainOrderFactory(
        reserved_to=MSK_TZ.localize(datetime(2018, 7, 19)), status=OrderStatus.PAYMENT_FAILED,
        partner_data=PartnerDataFactory(is_order_cancelled=False)
    )
    find_and_cancel_expired_orders(timedelta(minutes=10))
    assert not order1.reload().current_partner_data.is_order_cancelled
    assert not order2.reload().current_partner_data.is_order_cancelled
    assert not order3.reload().current_partner_data.is_order_cancelled


@replace_now('2018-07-18 16:00:00')
def test_is_order_cancelled():
    reserved_to = MSK_TZ.localize(datetime(2018, 7, 18, 15, 55))
    TrainOrderFactory(reserved_to=reserved_to, status=OrderStatus.DONE)
    TrainOrderFactory(reserved_to=reserved_to, status=OrderStatus.PAID)
    with mock.patch('travel.rasp.train_api.train_purchase.tasks.cancel_expired_orders.cancel_order') as m_cancel_order:
        find_and_cancel_expired_orders(timedelta(minutes=10))
        assert m_cancel_order.call_count == 0


@replace_now('2018-07-18 16:00:00')
def test_status_not_need_cancel():
    order1 = TrainOrderFactory(
        reserved_to=MSK_TZ.localize(datetime(2018, 7, 18, 15, 55)), status=OrderStatus.PAYMENT_FAILED,
        partner_data=PartnerDataFactory(is_order_cancelled=True)
    )
    with mock.patch('travel.rasp.train_api.train_purchase.tasks.cancel_expired_orders.cancel_order') as m_cancel_order:
        find_and_cancel_expired_orders(timedelta(minutes=10))
        assert m_cancel_order.call_count == 0
    assert order1.reload().current_partner_data.is_order_cancelled
