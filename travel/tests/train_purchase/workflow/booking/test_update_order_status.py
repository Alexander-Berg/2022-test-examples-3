# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
import pytz

from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.booking.update_order_status import UpdateOrderStatus, UpdateOrderStatusEvents

TEST_NOW = datetime(18, 9, 18, 13, 10, 30)

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def _process(order, order_status):
    return process_state_action({'action': UpdateOrderStatus, 'args': (order_status,)},
                                [UpdateOrderStatusEvents.OK], order)


@replace_now(TEST_NOW)
@pytest.mark.parametrize('order_status, expected_finished_at, expected_travel_status', [
    (OrderStatus.CANCELLED, None, TravelOrderStatus.CANCELLED),
    (OrderStatus.PAYMENT_FAILED, None, TravelOrderStatus.CANCELLED),
    (OrderStatus.DONE, None, TravelOrderStatus.DONE),
    (OrderStatus.PAID, TEST_NOW, TravelOrderStatus.IN_PROGRESS),
])
def test_do_update_order_status(order_status, expected_finished_at, expected_travel_status):
    order = TrainOrderFactory(status=OrderStatus.RESERVED)
    event, order = _process(order, order_status)
    assert event == UpdateOrderStatusEvents.OK
    assert order.status == order_status
    if expected_finished_at:
        assert order.finished_at == MSK_TZ.localize(expected_finished_at).astimezone(pytz.UTC).replace(tzinfo=None)
    else:
        assert order.finished_at is None
    assert order.travel_status == expected_travel_status
    assert order.travel_status == order.get_actual_travel_status()
