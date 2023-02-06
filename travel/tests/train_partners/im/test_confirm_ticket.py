# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.im.confirm_ticket import CANCEL_TICKET_ENDPOINT, cancel_order
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory


@pytest.mark.dbuser
def test_cancel_order(httpretty):
    mock_im(httpretty, CANCEL_TICKET_ENDPOINT, body='{}')

    cancel_order(TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=42)))

    request = httpretty.last_request
    assert request.body == '{"OrderId": 42}'
