# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from mock import patch

from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ, UTC_TZ
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.booking import set_pending
from travel.rasp.train_api.train_purchase.workflow.booking.set_pending import SetMaxPendingTill, SetMaxPendingTillEvents


@pytest.mark.dbuser
@pytest.mark.mongouser
@patch('settings.DEFAULT_PARTNER_TIMEOUT', 120)
@replace_now(datetime(2015, 1, 1, microsecond=5000, tzinfo=UTC_TZ).astimezone(MSK_TZ).replace(tzinfo=None))
@pytest.mark.parametrize('max_pending_till,check_order_delay_secs,awaited_event,awaited_max_pending_till', [
    (
        None,
        60,
        SetMaxPendingTillEvents.OK,
        datetime(2015, 1, 1, 0, 2),
    ),
    (
        datetime(2015, 1, 1, 0, 1),
        300,
        SetMaxPendingTillEvents.RETRY,
        datetime(2015, 1, 1, 0, 5),
    ),
])
def test_set_max_pending_till(max_pending_till, check_order_delay_secs, awaited_event,
                              awaited_max_pending_till):
    order = TrainOrderFactory(max_pending_till=max_pending_till)
    with patch.object(set_pending, 'CHECK_ORDER_DELAY_SECONDS', check_order_delay_secs):
        event, order = process_state_action(
            SetMaxPendingTill,
            (SetMaxPendingTillEvents.OK, SetMaxPendingTillEvents.RETRY),
            order
        )
    assert event == awaited_event
    assert order.max_pending_till == awaited_max_pending_till
