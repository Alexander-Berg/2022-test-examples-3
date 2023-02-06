# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, timedelta

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder

from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.tasks import collect_expired_orders
from travel.rasp.train_api.train_purchase.tasks.collect_expired_orders import find_orders, set_orders_expired

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def create_train_order(reserved_to=MSK_TZ.localize(datetime(2018, 7, 18, 15, 55)),
                       status=OrderStatus.CONFIRM_FAILED,
                       is_order_expired=None):
    return TrainOrderFactory(
        reserved_to=reserved_to, status=status, is_order_expired=is_order_expired
    )


@replace_now('2018-07-18 16:00:00')
def test_find():
    create_train_order(reserved_to=MSK_TZ.localize(datetime(2018, 7, 18, 16, 1)))
    create_train_order(reserved_to=MSK_TZ.localize(datetime(2018, 7, 18, 3)))
    create_train_order(status=OrderStatus.DONE)
    create_train_order(status=OrderStatus.PAID)
    create_train_order(is_order_expired=True)
    statuses_must_be_checked = [
        OrderStatus.PAYMENT_FAILED, OrderStatus.START_PAYMENT_FAILED, OrderStatus.PAYMENT_OUTDATED,
        OrderStatus.CONFIRM_FAILED, OrderStatus.CANCELLED, OrderStatus.RESERVED
    ]
    valid_orders = [create_train_order(status=s) for s in statuses_must_be_checked]
    orders = list(find_orders(timedelta(minutes=10)))
    assert_that(orders, contains_inanyorder(*valid_orders))


@replace_now('2018-07-18 16:00:00')
@mock.patch.object(collect_expired_orders, 'find_orders', autospec=True)
@mock.patch.object(collect_expired_orders, 'send_event_to_order', autospec=True)
def test_set_orders_expired(m_send_event_to_order, m_find_orders):
    orders = [create_train_order(status=s) for s in [OrderStatus.PAYMENT_OUTDATED, OrderStatus.CONFIRM_FAILED]]
    m_find_orders.return_value = orders
    set_orders_expired(timedelta(minutes=10))
    assert m_send_event_to_order.call_count == len(orders)
    for order in orders:
        assert order.reload().is_order_expired
