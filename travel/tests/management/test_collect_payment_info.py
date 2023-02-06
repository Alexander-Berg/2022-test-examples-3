# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, timedelta

import pytest
from bson import ObjectId
from hamcrest import assert_that, has_entries, contains, contains_inanyorder

from common.data_api.billing.trust_client import TrustPaymentStatuses
from common.tester.utils.datetime import replace_now
from common.utils.date import UTC_TZ, MSK_TZ
from travel.rasp.train_api.management.commands.collect_payment_info import payment_info_iter
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]
MSK_NOW = datetime(2018, 3, 19, 12)
UTC_NOW = MSK_TZ.localize(MSK_NOW).astimezone(UTC_TZ).replace(tzinfo=None)


@pytest.fixture(autouse=True)
def fix_now():
    with replace_now(MSK_NOW):
        yield


def test_payment_info_iter_by_date():
    order = TrainOrderFactory(
        id=ObjectId.from_datetime(UTC_NOW),
        status=OrderStatus.DONE,
        payments=[dict(
            purchase_token='actual',
            status=TrustPaymentStatuses.STARTED.value,
            resp_code='actual-resp-code',
            resp_desc='actual-resp-desc',
        )]
    )
    order_before = TrainOrderFactory(  # noqa
        id=ObjectId.from_datetime(UTC_NOW - timedelta(days=1)),
        status=OrderStatus.DONE,
        payments=[dict(purchase_token='before')]
    )
    order_after = TrainOrderFactory(  # noqa
        id=ObjectId.from_datetime(UTC_NOW + timedelta(days=1)),
        status=OrderStatus.DONE,
        payments=[dict(purchase_token='after')]
    )

    result = list(payment_info_iter(MSK_NOW.date(), statuses=[OrderStatus.DONE]))

    assert_that(result, contains(has_entries(
        order_uid=order.uid,
        order_status=OrderStatus.DONE.value,
        purchase_token=order.payments[0].purchase_token,
        payment_status=order.payments[0].status,
        payment_resp_code=order.payments[0].resp_code,
        payment_resp_desc=order.payments[0].resp_desc,
    )))


def test_payment_info_iter_by_statuses():
    order_done = TrainOrderFactory(
        id=ObjectId.from_datetime(UTC_NOW),
        status=OrderStatus.DONE,
        payments=[dict(purchase_token='done')]
    )
    order_canceled = TrainOrderFactory(  # noqa
        id=ObjectId.from_datetime(UTC_NOW + timedelta(minutes=1)),
        status=OrderStatus.CANCELLED,
        payments=[dict(purchase_token='canceled')]
    )
    order_payment_failed = TrainOrderFactory(
        id=ObjectId.from_datetime(UTC_NOW + timedelta(minutes=2)),
        status=OrderStatus.PAYMENT_FAILED,
        payments=[dict(purchase_token='payment_failed')]
    )

    result = list(payment_info_iter(MSK_NOW.date(), statuses=[OrderStatus.DONE, OrderStatus.PAYMENT_FAILED]))

    assert_that(result, contains_inanyorder(
        has_entries(
            order_uid=order_done.uid,
            order_status=OrderStatus.DONE.value,
            purchase_token='done'
        ),
        has_entries(
            order_uid=order_payment_failed.uid,
            order_status=OrderStatus.PAYMENT_FAILED.value,
            purchase_token='payment_failed'
        )
    ))


def test_payment_info_iter_in_history():
    order_done = TrainOrderFactory(
        id=ObjectId.from_datetime(UTC_NOW),
        status=OrderStatus.DONE,
        payments=[
            dict(purchase_token='third', trust_created_at=UTC_NOW - timedelta(minutes=1)),
            dict(purchase_token='second', trust_created_at=UTC_NOW - timedelta(minutes=2)),
            dict(purchase_token='first', trust_created_at=UTC_NOW - timedelta(minutes=3))
        ]
    )

    result = list(payment_info_iter(MSK_NOW.date(), statuses=[OrderStatus.DONE]))

    assert_that(result, contains_inanyorder(
        has_entries(
            order_uid=order_done.uid,
            order_status=OrderStatus.DONE.value,
            purchase_token='first'
        ),
        has_entries(
            order_uid=order_done.uid,
            order_status=OrderStatus.DONE.value,
            purchase_token='second'
        ),
        has_entries(
            order_uid=order_done.uid,
            order_status=OrderStatus.DONE.value,
            purchase_token='third'
        )
    ))
