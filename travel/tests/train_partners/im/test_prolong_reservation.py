# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_entries

from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_partners.im.factories.prolong_reservation import create_prolong_reservation_response
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.prolong_reservation import IM_PROLONG_RESERVATION_METHOD, prolong_reservation
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_prolong_reservation(httpretty):
    order = TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=42))
    im_confirm_till = MSK_TZ.localize(datetime(2018, 7, 16, 10, 30, 44))
    mock_im(httpretty, IM_PROLONG_RESERVATION_METHOD,
            json=create_prolong_reservation_response(order.current_partner_data.im_order_id, im_confirm_till))

    reserved_to = prolong_reservation(order)

    assert reserved_to == im_confirm_till
    assert_that(httpretty.last_request.parsed_body, has_entries(
        OrderId=order.current_partner_data.im_order_id,
        ProlongReservationType="RailwayThreeHoursReservation"
    ))
