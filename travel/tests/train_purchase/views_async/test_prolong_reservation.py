# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import pytest
from hamcrest import assert_that, has_entries

from common.dynamic_settings.default import conf
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.date import environment
from travel.rasp.train_api.train_partners.im.factories.prolong_reservation import create_prolong_reservation_response
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.prolong_reservation import IM_PROLONG_RESERVATION_METHOD
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory
from travel.rasp.train_api.train_purchase.views_async import try_to_prolong_reservation

STANDARD_RESERVE_MINUTES = 15

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


@pytest.fixture(autouse=True)
def freeze_now():
    with replace_now('2018-07-16 00:00:00'):
        yield


@replace_dynamic_setting('TRAIN_PURCHASE_PROLONG_RESERVATION_MINUTES', 10)
@pytest.mark.parametrize('partner_new_reservation_minutes', (5, 100))
def test_dynamic_setting_is_too_little(httpretty, partner_new_reservation_minutes):
    """ Малое значение динамической настройки, поэтому стандартное время бронирования """
    order = TrainOrderFactory(
        reserved_to=environment.now_utc() + timedelta(minutes=STANDARD_RESERVE_MINUTES),
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id=42, is_three_hours_reservation_available=True)
    )
    im_confirm_till = environment.now_aware() + timedelta(minutes=partner_new_reservation_minutes)
    mock_im(httpretty, IM_PROLONG_RESERVATION_METHOD,
            json=create_prolong_reservation_response(order.current_partner_data.im_order_id, im_confirm_till))

    try_to_prolong_reservation(order)

    assert order.reserved_to == environment.now_utc() + timedelta(minutes=STANDARD_RESERVE_MINUTES)
    assert not httpretty.latest_requests
    assert not order.current_partner_data.is_reservation_prolonged


@replace_dynamic_setting('TRAIN_PURCHASE_PROLONG_RESERVATION_MINUTES', 20)
def test_error_from_partner(httpretty):
    """ Ошибка при запросе к партнеру, поэтому стандартное время бронирования """
    order = TrainOrderFactory(
        reserved_to=environment.now_utc() + timedelta(minutes=STANDARD_RESERVE_MINUTES),
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id=42, is_three_hours_reservation_available=True)
    )
    mock_im(httpretty, IM_PROLONG_RESERVATION_METHOD, status=500,
            json={"Code": 666, "Message": "Error", "MessageParams": []})

    try_to_prolong_reservation(order)

    assert order.reserved_to == environment.now_utc() + timedelta(minutes=STANDARD_RESERVE_MINUTES)
    assert httpretty.last_request
    assert not order.current_partner_data.is_reservation_prolonged


@replace_dynamic_setting('TRAIN_PURCHASE_PROLONG_RESERVATION_MINUTES', 20)
def test_too_much_time_from_partner(httpretty):
    """ Партнер продлил бронь на долго, поэтому время бронирования из настройки """
    order = TrainOrderFactory(
        reserved_to=environment.now_utc() + timedelta(minutes=STANDARD_RESERVE_MINUTES),
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id=42, is_three_hours_reservation_available=True)
    )
    im_confirm_till = environment.now_aware() + timedelta(minutes=100500)
    mock_im(httpretty, IM_PROLONG_RESERVATION_METHOD,
            json=create_prolong_reservation_response(order.current_partner_data.im_order_id, im_confirm_till))

    try_to_prolong_reservation(order)

    expected_reserved_to = environment.now_utc() + timedelta(minutes=conf.TRAIN_PURCHASE_PROLONG_RESERVATION_MINUTES)
    assert order.reserved_to == expected_reserved_to
    assert_that(httpretty.last_request.parsed_body, has_entries(
        OrderId=order.current_partner_data.im_order_id,
        ProlongReservationType="RailwayThreeHoursReservation"
    ))
    assert order.current_partner_data.is_reservation_prolonged


@replace_dynamic_setting('TRAIN_PURCHASE_PROLONG_RESERVATION_MINUTES', 100500)
def test_too_much_time_from_dynamic_setting(httpretty):
    """ Партнер продлил бронь меньше чем настройка, берем время брони партнера """
    order = TrainOrderFactory(
        reserved_to=environment.now_utc() + timedelta(minutes=STANDARD_RESERVE_MINUTES),
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id=42, is_three_hours_reservation_available=True)
    )
    im_confirm_minutes = 30
    im_confirm_till = environment.now_aware() + timedelta(minutes=im_confirm_minutes)
    mock_im(httpretty, IM_PROLONG_RESERVATION_METHOD,
            json=create_prolong_reservation_response(order.current_partner_data.im_order_id, im_confirm_till))

    try_to_prolong_reservation(order)

    assert order.reserved_to == environment.now_utc() + timedelta(minutes=im_confirm_minutes)
    assert_that(httpretty.last_request.parsed_body, has_entries(
        OrderId=order.current_partner_data.im_order_id,
        ProlongReservationType="RailwayThreeHoursReservation"
    ))
    assert order.current_partner_data.is_reservation_prolonged


def test_no_prolongation_request_if_not_is_three_hours_reservation_available(httpretty):
    original_reserved_to = environment.now_utc() + timedelta(minutes=STANDARD_RESERVE_MINUTES)
    order = TrainOrderFactory(
        reserved_to=original_reserved_to,
        partner=TrainPartner.IM,
        partner_data=PartnerDataFactory(im_order_id=42, is_three_hours_reservation_available=False)
    )
    im_confirm_till = environment.now_aware() + timedelta(minutes=30)
    mock_im(httpretty, IM_PROLONG_RESERVATION_METHOD,
            json=create_prolong_reservation_response(order.current_partner_data.im_order_id, im_confirm_till))

    try_to_prolong_reservation(order)

    assert not httpretty.latest_requests
    assert order.reserved_to == original_reserved_to
    assert not order.current_partner_data.is_reservation_prolonged
