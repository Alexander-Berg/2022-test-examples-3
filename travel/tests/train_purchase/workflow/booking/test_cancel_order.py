# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_partners.ufs.confirm_ticket import CONFIRM_TICKET_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.booking.cancel_order import CancelOrder, CancelOrderEvents

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def _process(order):
    return process_state_action({'action': CancelOrder}, [CancelOrderEvents.OK], order)


@pytest.mark.parametrize('xml', [
    '<UFS_RZhD_Gate><Status>0</Status></UFS_RZhD_Gate>',
    '<UFS_RZhD_Gate><Status>1</Status></UFS_RZhD_Gate>',
])
def test_cancel_order_without_payment(httpretty, xml):
    order = TrainOrderFactory(partner=TrainPartner.UFS, payments=[dict(purchase_token=None)])
    mock_ufs(httpretty, CONFIRM_TICKET_ENDPOINT, body=xml)
    event, order = _process(order)
    assert event == CancelOrderEvents.OK
    assert order.current_partner_data.is_order_cancelled


def test_cancel_order_with_cancel_exception(httpretty):
    order = TrainOrderFactory(partner=TrainPartner.UFS, payments=[dict(purchase_token=None)])
    mock_ufs(httpretty, CONFIRM_TICKET_ENDPOINT, body='<UFS_RZhD_Gate><Error/></UFS_RZhD_Gate>')
    event, _order = _process(order)

    assert event == CancelOrderEvents.OK
    assert not order.current_partner_data.is_order_cancelled
