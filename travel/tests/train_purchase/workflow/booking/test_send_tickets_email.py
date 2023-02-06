# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase import workflow
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory
from travel.rasp.train_api.train_purchase.workflow.booking.send_tickets_email import SendTicketsEmail, SendTicketsEmailEvents


@pytest.mark.dbuser
@pytest.mark.mongouser
@mock.patch.object(workflow.booking.send_tickets_email, 'send_tickets_email', autospec=True)
def test_send_tickets_email(m_send_tickets_email):
    order = TrainOrderFactory(status=OrderStatus.RESERVED, partner_data=PartnerDataFactory(order_num='100500'))
    event, _order = process_state_action(SendTicketsEmail, (SendTicketsEmailEvents.OK,), order)

    assert event == SendTicketsEmailEvents.OK
    m_send_tickets_email.assert_called_once_with(order.uid)
