# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import re
from datetime import datetime, timedelta
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, has_entries, contains, not_, has_key

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.date.environment import now_aware
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult, PassengerInfo
from travel.rasp.train_api.train_partners.im.base import ImError
from travel.rasp.train_api.train_partners.im.factories.create_reservation import ImCreateReservationFactory
from travel.rasp.train_api.train_purchase.core.enums import RebookingStatus, AgeGroup, OrderStatus, InsuranceStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, RebookingInfoFactory, PassengerFactory, TicketFactory, TicketPaymentFactory,
    PassengerRebookingInfoFactory, InsuranceProcessFactory
)
from travel.rasp.train_api.train_purchase.core.models import PassengerTariffInfo
from travel.rasp.train_api.train_purchase.utils import order_tickets
from travel.rasp.train_api.train_purchase.workflow.booking import rebook_order as rebook_module
from travel.rasp.train_api.train_purchase.workflow.booking.rebook_order import RebookOrder, RebookOrderEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]
RESERVATION_URI = re.compile(r'.*/Reservation/Create')


def _process(order):
    return process_state_action(
        RebookOrder,
        (RebookOrderEvents.DONE, RebookOrderEvents.SKIPPED, RebookOrderEvents.FAILED),
        order,
    )


@pytest.fixture(autouse=True)
def _freeze_time_and_rebook_constants():
    with replace_now(
            '2019-05-01 12:00:00',
    ), replace_dynamic_setting(
        'TRAIN_PURCHASE_RESERVATION_MAX_CYCLES', 2,
    ), replace_dynamic_setting(
        'TRAIN_PURCHASE_RESERVATION_PARTNER_TIMEOUT', 20,
    ):
        yield


@pytest.fixture(autouse=True, name='m_cancel_order')
def _mock_cancel_order():
    with mock.patch.object(order_tickets, 'cancel_order', autospec=True) as m_cancel_order:
        yield m_cancel_order


def _create_order_info_result(reserved_to):
    return OrderInfoResult(
        buy_operation_id=None,
        expire_set_er=None,
        status=None,
        order_num=None,
        reserved_to=reserved_to,
        passengers=[
            PassengerInfo(
                blank_id='300',
                doc_id='100500',
                birth_date=datetime(1980, 1, 1),
                customer_id='old customer id',
            ),
        ],
    )


def _create_reserve_response(amount=100):
    data = ImCreateReservationFactory(**{
        'OrderId': 200,
        'Customers': [{
            'Index': 0,
            'OrderCustomerId': 'new customer id',
        }],
        'ReservationResults': [{
            'Passengers': [{
                'OrderCustomerReferenceIndex': 0,
                'OrderItemBlankId': 'new blank id',
                'Amount': amount,
                'PlacesWithType': [{'Number': '1', 'Type': 'NotNearTable'}]
            }],
            'Blanks': [{
                'OrderItemBlankId': 'new blank id',
            }]
        }],
    })
    return json.dumps(data)


def _make_passengers():
    passengers = [
        PassengerFactory(
            customer_id='old customer id',
            tickets=[
                TicketFactory(places=['1'], payment=TicketPaymentFactory(amount=Decimal(100))),
            ],
            rebooking_info=PassengerRebookingInfoFactory(
                age_group=AgeGroup.BABIES,
                tariff_info=PassengerTariffInfo(im_request_code='im request code'),
            ),
        ),
    ]
    return passengers


@pytest.mark.parametrize('cycle_number, expected_cycle_number, reserved_to_delta', [
    (None, 1, timedelta(minutes=1)),
    (1, 2, timedelta(minutes=-1)),
])
def test_rebook_order_done(m_cancel_order, cycle_number, expected_cycle_number, reserved_to_delta, httpretty):
    httpretty.register_uri(httpretty.POST, RESERVATION_URI, body=_create_reserve_response())

    order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=True,
            cycle_until=datetime(2019, 5, 1, 8, 55),
            cycle_number=cycle_number,
            give_child_without_place=True,
            additional_place_requirements='additional requirements',
        ),
    )

    with mock.patch.object(
            rebook_module, 'get_order_info', autospec=True,
            return_value=_create_order_info_result(reserved_to=now_aware() + reserved_to_delta),
    ) as m_get_order_info:
        status, update_spec = RebookOrder.rebook_order(order)

    assert status == RebookingStatus.DONE
    assert m_get_order_info.call_count == 1
    assert m_cancel_order.call_count == 1

    assert_that(httpretty.last_request.parsed_body, has_entries({
        'ReservationItems': contains(
            has_entries({
                'GiveAdditionalTariffForChildIfPossible': True,
                'Passengers': contains(
                    has_entries({
                        'Category': 'BabyWithoutPlace',
                        'PreferredAdultTariffType': 'im request code',
                    }),
                ),
            }),
        ),
    }))

    assert_that(update_spec, has_entries({
        'set__rebooking_info__cycle_until': datetime(2019, 5, 1, 9, 20),
        'set__rebooking_info__cycle_number': expected_cycle_number,
        'set__passengers__0__tickets__0__blank_id': 'new blank id',
        'set__passengers__0__customer_id': 'new customer id',
        'push__partner_data_history': has_entries({
            'im_order_id': 200,
            'reservation_datetime': datetime(2019, 5, 1, 9),
        }),
    }))


@pytest.mark.parametrize('expected_status, enabled, cycle_until, cycle_number, status', [
    (RebookingStatus.DISABLED, False, datetime(2019, 5, 1, 8, 55), 0, OrderStatus.RESERVED),
    (RebookingStatus.SKIPPED, True, datetime(2019, 5, 1, 9, 5), 0, OrderStatus.RESERVED),
    (RebookingStatus.OVERLIMIT, True, datetime(2019, 5, 1, 8, 55), 2, OrderStatus.RESERVED),
    (RebookingStatus.INVALID_ORDER, True, datetime(2019, 5, 1, 8, 55), 0, OrderStatus.DONE),
])
def test_rebook_order_impossible(expected_status, enabled, cycle_until, cycle_number, status):
    order = TrainOrderFactory(
        rebooking_info=RebookingInfoFactory(
            enabled=enabled,
            cycle_until=cycle_until,
            cycle_number=cycle_number,
        ),
        status=status,
    )

    status, update_spec = RebookOrder.rebook_order(order)

    assert status == expected_status
    assert not update_spec


def test_rebook_order_fail(httpretty):
    httpretty.register_uri(httpretty.POST, RESERVATION_URI, body='wrong answer')

    order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=True,
            cycle_until=datetime(2019, 5, 1, 8, 55),
        ),
    )

    with mock.patch.object(
            rebook_module, 'get_order_info', autospec=True,
            return_value=_create_order_info_result(reserved_to=now_aware()),
    ) as m_get_order_info:
        status, update_spec = RebookOrder.rebook_order(order)

    assert status == RebookingStatus.FAILED
    assert m_get_order_info.call_count == 1
    assert not update_spec


@pytest.mark.parametrize('error_code, expected_status', [
    (61, RebookingStatus.DONE),
    (1, RebookingStatus.FAILED),
])
def test_rebook_order_cancel_failed(httpretty, error_code, expected_status):
    httpretty.register_uri(httpretty.POST, RESERVATION_URI, body=_create_reserve_response())

    order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=True,
            cycle_until=datetime(2019, 5, 1, 8, 55),
        ),
    )

    with mock.patch.object(
            rebook_module, 'get_order_info', autospec=True,
            return_value=_create_order_info_result(reserved_to=now_aware() + timedelta(minutes=1)),
    ), mock.patch.object(
        order_tickets, 'cancel_order', autospec=True, side_effect=[ImError(error_code, 'Some partner error', None)]
    ) as m_cancel_order:
        status, update_spec = RebookOrder.rebook_order(order)

    assert status == expected_status
    assert m_cancel_order.call_count == 1


def test_rebook_order_mismatch(httpretty):
    httpretty.register_uri(httpretty.POST, RESERVATION_URI, body=_create_reserve_response(amount=200))

    order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=True,
            cycle_until=datetime(2019, 5, 1, 8, 55),
        ),
    )

    with mock.patch.object(
            rebook_module, 'get_order_info', autospec=True,
            return_value=_create_order_info_result(reserved_to=now_aware()),
    ) as m_get_order_info:
        status, update_spec = RebookOrder.rebook_order(order)

    assert status == RebookingStatus.MISMATCH
    assert m_get_order_info.call_count == 1
    assert_that(update_spec, has_entries({
        'set__passengers__0__tickets__0__blank_id': 'new blank id',
        'set__passengers__0__customer_id': 'new customer id',
        'push__partner_data_history': has_entries({
            'im_order_id': 200,
            'reservation_datetime': datetime(2019, 5, 1, 9),
        }),
    }))


@pytest.mark.parametrize('rebooking_enabled, insurance_status, expected_insurance_status', [
    (True, InsuranceStatus.CHECKED_OUT, InsuranceStatus.ACCEPTED),
    (False, InsuranceStatus.CHECKED_OUT, None),
    (True, InsuranceStatus.ACCEPTED, None),
    (True, InsuranceStatus.DECLINED, None),
])
def test_rebook_order_with_insurance(httpretty, rebooking_enabled, insurance_status, expected_insurance_status):
    httpretty.register_uri(httpretty.POST, RESERVATION_URI, body=_create_reserve_response())

    order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=rebooking_enabled,
            cycle_until=datetime(2019, 5, 1, 8, 55),
        ),
        insurance=InsuranceProcessFactory(
            status=insurance_status,
        ),
    )

    with mock.patch.object(
            rebook_module, 'get_order_info', autospec=True,
            return_value=_create_order_info_result(reserved_to=now_aware()),
    ):
        status, update_spec = RebookOrder.rebook_order(order)

    assert status == RebookingStatus.DONE if rebooking_enabled else RebookingStatus.DISABLED
    if not expected_insurance_status:
        assert_that(update_spec, not_(has_key('set__insurance__status')))
    else:
        assert_that(update_spec, has_entries({
            'set__insurance__status': expected_insurance_status,
        }))


def test_rebook_order_event_skipped():
    original_order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=False,
            cycle_until=datetime(2019, 5, 1, 8, 55),
        ),
    )

    event, order = _process(original_order)

    assert event == RebookOrderEvents.SKIPPED
    assert order.rebooking_info.status == RebookingStatus.DISABLED
    assert order.status == OrderStatus.RESERVED


def test_rebook_order_event_failed():
    original_order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=True,
            cycle_until=datetime(2019, 5, 1, 8, 55),
            cycle_number=2,
        ),
    )

    event, order = _process(original_order)

    assert event == RebookOrderEvents.FAILED
    assert order.rebooking_info.status == RebookingStatus.OVERLIMIT
    assert order.status == OrderStatus.CONFIRM_FAILED


def test_rebook_order_event_done(httpretty):
    httpretty.register_uri(httpretty.POST, RESERVATION_URI, body=_create_reserve_response())
    original_order = TrainOrderFactory(
        passengers=_make_passengers(),
        rebooking_info=RebookingInfoFactory(
            enabled=True,
            cycle_until=datetime(2019, 5, 1, 8, 55),
        ),
    )

    with mock.patch.object(
            rebook_module, 'get_order_info', autospec=True,
            return_value=_create_order_info_result(reserved_to=now_aware()),
    ):
        event, order = _process(original_order)

    assert event == RebookOrderEvents.DONE
    assert order.rebooking_info.status == RebookingStatus.DONE
    assert order.status == OrderStatus.RESERVED
