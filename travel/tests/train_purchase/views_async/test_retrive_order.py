# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from travel.rasp.train_api.train_partners import im
from travel.rasp.train_api.train_partners.im.factories.order_info import ImOrderInfoFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.ufs.get_order_info import TRANSACTION_INFO_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_partners.ufs.test_utils.response_factories import make_trans_info_response
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PassengerFactory, TicketFactory

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]


def test_get_order_update_pending_tickets(httpretty, async_urlconf_client):
    order = TrainOrderFactory(partner=TrainPartner.UFS,
                              passengers=[PassengerFactory(tickets=[TicketFactory(pending=True)])])

    mock_ufs(httpretty, TRANSACTION_INFO_ENDPOINT, body=make_trans_info_response(order, {
        'blanks': {order.passengers[0].tickets[0].blank_id: {'pending': False}}
    }))

    response = async_urlconf_client.get('/ru/api/train-purchase/orders/{}/'.format(order.uid))

    assert response.status_code == 200
    order.reload()

    assert not order.passengers[0].tickets[0].pending


@mock.patch.object(im, 'get_route_info', return_value=None, autospec=True)
def test_get_order_update_pending_tickets_im(m_, httpretty, async_urlconf_client):
    order = TrainOrderFactory(partner=TrainPartner.IM,
                              passengers=[PassengerFactory(tickets=[TicketFactory(pending=True)])])

    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(train_order=order))

    response = async_urlconf_client.get('/ru/api/train-purchase/orders/{}/'.format(order.uid))

    assert response.status_code == 200
    order.reload()

    assert not order.passengers[0].tickets[0].pending
