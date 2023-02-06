# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainRefundFactory
from travel.rasp.train_api.train_purchase.workflow.ticket_refund.unset_is_active import UnsetIsActive, UnsetIsActiveRefundEvents


def _process(refund):
    return process_state_action(UnsetIsActive, (UnsetIsActiveRefundEvents.DONE,), refund)


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_ok():
    old_refund = TrainRefundFactory(is_active=False, factory_extra_params={
        'create_order': True, 'create_order_kwargs': {'travel_status': TravelOrderStatus.RESERVED}
    })
    refund = TrainRefundFactory(is_active=True, order_uid=old_refund.order_uid)

    event, refund = _process(refund)

    assert event == UnsetIsActiveRefundEvents.DONE
    assert not refund.is_active
    assert refund.order.travel_status == TravelOrderStatus.DONE
