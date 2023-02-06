# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

from common.tester.utils.datetime import replace_now
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, RebookingInfoFactory
from travel.rasp.train_api.train_purchase.workflow.payment import rebook_order
from travel.rasp.train_api.train_purchase.workflow.payment.rebook_order import RebookOrder, RebookOrderEvents
from travel.rasp.train_api.train_purchase.workflow.user_events import TrainBookingUserEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _process(payment):
    return process_state_action(
        RebookOrder,
        (RebookOrderEvents.DONE, RebookOrderEvents.SKIPPED),
        payment,
    )


@replace_now('2019-06-01 16:00:00')
@mock.patch.object(rebook_order, 'send_event_to_order', autospec=True)
@pytest.mark.parametrize('cycle_until, expected_event, expected_sent_event', [
    (None, RebookOrderEvents.SKIPPED, None),
    (datetime(2019, 6, 2), RebookOrderEvents.SKIPPED, None),
    (datetime(2019, 6, 1), RebookOrderEvents.DONE, TrainBookingUserEvents.REBOOKING),
])
def test_rebook_order_event(m_send_event_to_order, cycle_until, expected_event, expected_sent_event):
    original_order = TrainOrderFactory(
        rebooking_info=RebookingInfoFactory(cycle_until=cycle_until),
    )
    event, _ = _process(original_order.current_billing_payment)

    assert event == expected_event
    if not expected_sent_event:
        assert not m_send_event_to_order.call_count
    else:
        m_send_event_to_order.assert_called_once_with(original_order, expected_sent_event)
