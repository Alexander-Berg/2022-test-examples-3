# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest
from hamcrest import assert_that, has_properties

from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ, UTC_TZ
from travel.rasp.train_api.train_purchase.core.factories import RefundEmailFactory, TrainRefundFactory
from travel.rasp.train_api.train_purchase.core.models import RefundEmail

pytestmark = [pytest.mark.mongouser('module')]
FAKE_NOW = datetime(2017, 11, 9, 10)
FAKE_UTC_NOW = MSK_TZ.localize(FAKE_NOW).astimezone(UTC_TZ).replace(tzinfo=None)


@pytest.yield_fixture(autouse=True)
def fix_now():
    with replace_now(FAKE_NOW):
        yield


@pytest.mark.dbuser
def test_create_intent():
    refund = TrainRefundFactory(factory_extra_params={'create_order': True})
    refund_email = RefundEmail.get_or_create_intent(refund.order_uid, refund.uuid)

    assert_that(refund_email, has_properties(
        order_uid=refund.order_uid,
        refund_uuid=refund.uuid,
        is_sent=False,
        created_at=FAKE_UTC_NOW,
        sent_at=None
    ))

    # для is_sent прописано default=False, поэтому надо дополнительно проверить, что и в монгу записали False
    assert RefundEmail.objects(order_uid=refund.order_uid, refund_uuid=refund.uuid, is_sent=False).count() == 1


@pytest.mark.dbuser
def test_get_intent():
    refund = TrainRefundFactory(factory_extra_params={'create_order': True})
    created_at = FAKE_UTC_NOW + timedelta(days=1)
    RefundEmailFactory(order_uid=refund.order_uid, refund_uuid=refund.uuid, created_at=created_at)
    refund_email = RefundEmail.get_or_create_intent(refund.order_uid, refund.uuid)

    assert_that(refund_email, has_properties(
        order_uid=refund.order_uid,
        refund_uuid=refund.uuid,
        is_sent=False,
        created_at=created_at,
        sent_at=None
    ))


def test_mark_success():
    refund_email = RefundEmailFactory()
    assert not refund_email.is_sent

    RefundEmail.mark_success(refund_email.order_uid, refund_email.refund_uuid)
    refund_email = RefundEmail.get_or_create_intent(refund_email.order_uid, refund_email.refund_uuid)

    assert_that(refund_email, has_properties(
        is_sent=True,
        sent_at=FAKE_UTC_NOW
    ))
