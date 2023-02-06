# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import mock
import pytest
from hamcrest import assert_that, has_entries, contains

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.date import environment
from travel.rasp.train_api.train_purchase.core.enums import TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainRefundFactory
from travel.rasp.train_api.train_purchase.core.models import RefundStatus
from travel.rasp.train_api.train_purchase.tasks import unhandled_exception_refund
from travel.rasp.train_api.train_purchase.tasks.unhandled_exception_refund import process_unhandled_exception_refund

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@replace_now('2019-02-26')
@replace_setting('UNHANDLED_EXCEPTION_ORDER_CAMPAIGN', None)
@mock.patch.object(unhandled_exception_refund, 'guaranteed_send_email', autospec=True)
@pytest.mark.parametrize('process_state, created_timedelta_before_now, is_email_expected', [
    ('unhandled_exception_state', timedelta(days=7), False),
    ('unhandled_exception_state', timedelta(days=6, hours=23), True),
    ('unhandled_exception_state', timedelta(seconds=0), True),
    ('done', timedelta(days=7), False),
    ('done', timedelta(days=1), False),
])
def test_send_unhandled_exception_order_email_due_order(m_guaranteed_send_email,
                                                        process_state, created_timedelta_before_now,
                                                        is_email_expected):
    refund = TrainRefundFactory(
        created_at=environment.now_utc() - created_timedelta_before_now,
        process={'state': process_state},
        factory_extra_params={'create_order': True,
                              'create_order_kwargs': {'travel_status': TravelOrderStatus.IN_PROGRESS}},
        status=RefundStatus.NEW,
    )

    process_unhandled_exception_refund()
    refund.reload()
    order = refund.order

    if not is_email_expected:
        assert not refund.problem_refund_email_is_sent
        assert not m_guaranteed_send_email.called
        assert order.travel_status == TravelOrderStatus.IN_PROGRESS
    else:
        assert refund.problem_refund_email_is_sent
        assert m_guaranteed_send_email.call_count == 1
        assert_that(m_guaranteed_send_email.call_args, contains(
            (),  # args
            has_entries(  # kwargs
                args=has_entries(order_uid=refund.order_uid, process='refund', refund_status=RefundStatus.NEW),
                key='send_unhandled_exception_refund_email_{}'.format(refund.uuid),
            )
        ))
        assert order.travel_status == TravelOrderStatus.DONE
