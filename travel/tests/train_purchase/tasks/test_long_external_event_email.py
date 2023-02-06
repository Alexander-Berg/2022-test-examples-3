# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import mock
import pytest
from hamcrest import assert_that, has_entries, contains

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.date import environment
from travel.rasp.train_api.train_purchase.core.factories import PaymentFactory, TrainOrderFactory
from travel.rasp.train_api.train_purchase.tasks import long_external_event_email
from travel.rasp.train_api.train_purchase.tasks.long_external_event_email import send_long_external_event_email

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@replace_now('2018-07-07')
@replace_setting('LONG_EXTERNAL_EVENT_CAMPAIGN', None)
@mock.patch.object(long_external_event_email, 'guaranteed_send_email', autospec=True)
@pytest.mark.parametrize('is_external, created_at_timedelta_before_now, email_already_sent, is_email_expected', [
    (True, timedelta(minutes=6), False, True),
    (True, timedelta(minutes=0), False, False),
    (True, timedelta(minutes=6), True, False),
    (False, timedelta(minutes=6), False, False),
    (True, None, False, False),
])
def test_send_long_external_event_email_due_order(
        m_guaranteed_send_email, is_external, created_at_timedelta_before_now, email_already_sent, is_email_expected,
):
    order = TrainOrderFactory(
        process={
            'created_at': (
                environment.now_utc() - created_at_timedelta_before_now if created_at_timedelta_before_now else None
            ),
            'received_event': {'is_external': is_external},
        },
        email_flags={'problem_order_email_is_sent': email_already_sent},
    )

    send_long_external_event_email()
    order.reload()

    if not is_email_expected:
        if not email_already_sent:
            assert not order.email_flags or not order.email_flags.problem_order_email_is_sent
        assert not m_guaranteed_send_email.called
    else:
        assert order.email_flags.problem_order_email_is_sent
        assert m_guaranteed_send_email.call_count == 1
        assert_that(m_guaranteed_send_email.call_args, contains(
            (),  # args
            has_entries(  # kwargs
                args=has_entries(order_uid=order.uid, process='booking'),
                key='send_long_external_event_email_{}'.format(order.uid)
            )
        ))


@replace_now('2018-07-07')
@replace_setting('LONG_EXTERNAL_EVENT_CAMPAIGN', None)
@mock.patch.object(long_external_event_email, 'guaranteed_send_email', autospec=True)
@pytest.mark.parametrize('is_external, created_at_timedelta_before_now, email_already_sent, is_email_expected', [
    (True, timedelta(minutes=6), False, True),
    (True, timedelta(minutes=0), False, False),
    (True, timedelta(minutes=6), True, False),
    (False, timedelta(minutes=6), False, False),
    (True, None, False, False),
])
def test_send_long_external_event_email_due_payment(
        m_guaranteed_send_email, is_external, created_at_timedelta_before_now, email_already_sent, is_email_expected,
):
    order = TrainOrderFactory(email_flags={'problem_order_email_is_sent': email_already_sent})
    PaymentFactory(
        order_uid=order.uid,
        process={
            'created_at': (
                environment.now_utc() - created_at_timedelta_before_now if created_at_timedelta_before_now else None
            ),
            'received_event': {'is_external': is_external},
        },
    )

    send_long_external_event_email()
    order.reload()

    if not is_email_expected:
        if not email_already_sent:
            assert not order.email_flags or not order.email_flags.problem_order_email_is_sent
        assert not m_guaranteed_send_email.called
    else:
        assert order.email_flags.problem_order_email_is_sent
        assert m_guaranteed_send_email.call_count == 1
        assert_that(m_guaranteed_send_email.call_args, contains(
            (),  # args
            has_entries(  # kwargs
                args=has_entries(order_uid=order.uid, process='payment'),
                key='send_long_external_event_email_{}'.format(order.uid)
            )
        ))


@replace_now('2018-07-07')
@replace_setting('LONG_EXTERNAL_EVENT_CAMPAIGN', None)
@mock.patch.object(long_external_event_email, 'guaranteed_send_email', autospec=True)
def test_send_long_external_event_email_due_order_and_payment(m_guaranteed_send_email):
    order = TrainOrderFactory(
        reserved_to=environment.now_utc(),
        process={'created_at': environment.now_utc() - timedelta(minutes=6),
                 'received_event': {'is_external': True}}
    )
    PaymentFactory(
        order_uid=order.uid,
        trust_created_at=environment.now_utc(),
        process={'created_at': environment.now_utc() - timedelta(minutes=6),
                 'received_event': {'is_external': True}}
    )

    send_long_external_event_email()
    order.reload()

    assert order.email_flags.problem_order_email_is_sent
    assert m_guaranteed_send_email.call_count == 1
    assert_that(m_guaranteed_send_email.call_args, contains(
        (),  # args
        has_entries(  # kwargs
            args=has_entries(order_uid=order.uid, process='payment'),
            key='send_long_external_event_email_{}'.format(order.uid)
        )
    ))
