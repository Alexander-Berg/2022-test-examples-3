# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import mock
import pytest
from hamcrest import assert_that, has_entries, contains

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.date import environment
from travel.rasp.train_api.train_purchase.core.enums import TravelOrderStatus, OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import PaymentFactory, TrainOrderFactory
from travel.rasp.train_api.train_purchase.tasks import unhandled_exception_order
from travel.rasp.train_api.train_purchase.tasks.unhandled_exception_order import process_unhandled_exception_orders

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@replace_now('2018-07-07')
@replace_setting('UNHANDLED_EXCEPTION_ORDER_CAMPAIGN', None)
@mock.patch.object(unhandled_exception_order, 'guaranteed_send_email', autospec=True)
@pytest.mark.parametrize('process_state, reserved_timedelta_before_now, is_email_expected', [
    ('unhandled_exception_state', timedelta(days=7), False),
    ('unhandled_exception_state', timedelta(days=6, hours=23), True),
    ('unhandled_exception_state', timedelta(seconds=0), True),
    ('done', timedelta(days=7), False),
    ('done', timedelta(days=6, hours=23), False),
])
def test_process_unhandled_exception_orders_due_order(m_guaranteed_send_email,
                                                      process_state, reserved_timedelta_before_now,
                                                      is_email_expected):
    order = TrainOrderFactory(
        reserved_to=environment.now_utc() - reserved_timedelta_before_now,
        process={'state': process_state},
        travel_status=TravelOrderStatus.RESERVED,
    )

    process_unhandled_exception_orders()
    order.reload()

    if not is_email_expected:
        assert not order.email_flags or not order.email_flags.problem_order_email_is_sent
        assert not m_guaranteed_send_email.called
        assert order.travel_status == TravelOrderStatus.RESERVED
    else:
        assert order.email_flags.problem_order_email_is_sent
        assert m_guaranteed_send_email.call_count == 1
        assert_that(m_guaranteed_send_email.call_args, contains(
            (),  # args
            has_entries(  # kwargs
                args=has_entries(order_uid=order.uid, process='booking'),
                key='send_unhandled_exception_order_email_{}'.format(order.uid)
            )
        ))
        assert order.travel_status == TravelOrderStatus.CANCELLED


@replace_now('2018-07-07')
@replace_setting('UNHANDLED_EXCEPTION_ORDER_CAMPAIGN', None)
@mock.patch.object(unhandled_exception_order, 'guaranteed_send_email', autospec=True)
def test_process_unhandled_exception_order_state_done(m_guaranteed_send_email):
    order = TrainOrderFactory(
        reserved_to=environment.now_utc() - timedelta(days=1),
        process={'state': 'unhandled_exception_state'},
        travel_status=TravelOrderStatus.DONE,
        status=OrderStatus.DONE,
    )

    process_unhandled_exception_orders()
    order.reload()

    assert order.email_flags.problem_order_email_is_sent
    assert m_guaranteed_send_email.call_count == 1
    assert_that(m_guaranteed_send_email.call_args, contains(
        (),  # args
        has_entries(  # kwargs
            args=has_entries(order_uid=order.uid, process='booking'),
            key='send_unhandled_exception_order_email_{}'.format(order.uid)
        )
    ))
    assert order.travel_status == TravelOrderStatus.DONE


@replace_now('2018-07-07')
@replace_setting('UNHANDLED_EXCEPTION_ORDER_CAMPAIGN', None)
@mock.patch.object(unhandled_exception_order, 'guaranteed_send_email', autospec=True)
@pytest.mark.parametrize('process_state, created_timedelta_before_now, email_already_sent, is_email_expected', [
    ('unhandled_exception_state', timedelta(days=7), False, False),
    ('unhandled_exception_state', timedelta(days=6, hours=23), False, True),
    ('unhandled_exception_state', timedelta(seconds=0), False, True),
    ('unhandled_exception_state', timedelta(seconds=0), True, False),
    ('done', timedelta(days=6, hours=23), False, False),
])
def test_process_unhandled_exception_orders_due_payment(m_guaranteed_send_email, process_state,
                                                        created_timedelta_before_now, email_already_sent,
                                                        is_email_expected):
    order = TrainOrderFactory(
        email_flags={'problem_order_email_is_sent': email_already_sent},
        travel_status=TravelOrderStatus.RESERVED,
    )
    PaymentFactory(order_uid=order.uid,
                   trust_created_at=environment.now_utc() - created_timedelta_before_now,
                   process={'state': process_state})

    process_unhandled_exception_orders()
    order.reload()

    if not is_email_expected:
        if not email_already_sent:
            assert not order.email_flags or not order.email_flags.problem_order_email_is_sent
        assert not m_guaranteed_send_email.called
        assert order.travel_status == TravelOrderStatus.RESERVED
    else:
        assert order.email_flags.problem_order_email_is_sent
        assert m_guaranteed_send_email.call_count == 1
        assert_that(m_guaranteed_send_email.call_args, contains(
            (),  # args
            has_entries(  # kwargs
                args=has_entries(order_uid=order.uid, process='payment'),
                key='send_unhandled_exception_order_email_{}'.format(order.uid)
            )
        ))
        assert order.travel_status == TravelOrderStatus.CANCELLED


@replace_now('2018-07-07')
@replace_setting('UNHANDLED_EXCEPTION_ORDER_CAMPAIGN', None)
@mock.patch.object(unhandled_exception_order, 'guaranteed_send_email', autospec=True)
def test_process_unhandled_exception_orders_due_order_and_payment(m_guaranteed_send_email):
    order = TrainOrderFactory(
        reserved_to=environment.now_utc(), process={'state': 'unhandled_exception_state'},
        travel_status=TravelOrderStatus.RESERVED,
    )
    PaymentFactory(order_uid=order.uid, trust_created_at=environment.now_utc(),
                   process={'state': 'unhandled_exception_state'})

    process_unhandled_exception_orders()
    order.reload()

    assert order.email_flags.problem_order_email_is_sent
    assert m_guaranteed_send_email.call_count == 1
    assert_that(m_guaranteed_send_email.call_args, contains(
        (),  # args
        has_entries(  # kwargs
            args=has_entries(order_uid=order.uid, process='payment'),
            key='send_unhandled_exception_order_email_{}'.format(order.uid)
        )
    ))
    assert order.travel_status == TravelOrderStatus.CANCELLED
