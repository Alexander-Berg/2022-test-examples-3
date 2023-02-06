# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest
import pytz
from hamcrest import assert_that, has_entries

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_purchase.core.models import (
    RefundPayment, RefundPaymentStatus
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class TestPendingRefundPaymentView(object):
    @pytest.mark.parametrize('status, total', [
        (RefundPaymentStatus.UNKNOWN, 1),
        (RefundPaymentStatus.NEW, 1),
        (RefundPaymentStatus.FAILED, 0),
        (RefundPaymentStatus.DONE, 0),
    ])
    @replace_now('2017-10-10 20:00:00')
    @replace_setting('BYPASS_BACKOFFICE_AUTH', True)
    def test_statuses(self, async_urlconf_client, status, total):
        dt = MSK_TZ.localize(datetime(2017, 10, 10, 19, 40)).astimezone(pytz.UTC)
        RefundPayment.objects.create(order_uid='1' * 32, refund_uuid='1' * 32, refund_created_at=dt,
                                     refund_payment_status=status, refund_blank_ids=['1'])
        data = async_urlconf_client.get('/ru/train-purchase-backoffice/pending-refund-payments/').data
        assert data['total'] == total

    @replace_now('2017-10-10 20:00:00')
    @replace_setting('BYPASS_BACKOFFICE_AUTH', True)
    def test_response_and_minutes_passed(self, async_urlconf_client):
        dt = MSK_TZ.localize(datetime(2017, 10, 10, 19, 40)).astimezone(pytz.UTC)
        RefundPayment.objects.create(order_uid='1' * 32, refund_uuid='1' * 32, refund_created_at=dt,
                                     refund_payment_status=RefundPaymentStatus.UNKNOWN, refund_blank_ids=['1'])
        RefundPayment.objects.create(order_uid='2' * 32, refund_uuid='2' * 32,
                                     refund_created_at=dt + timedelta(minutes=10),
                                     refund_payment_status=RefundPaymentStatus.UNKNOWN, refund_blank_ids=['1'])
        data = async_urlconf_client.get('/ru/train-purchase-backoffice/pending-refund-payments/').data
        assert data['total'] == 1
        assert_that(data['payments'][0], has_entries({
            'orderUID': '1' * 32,
            'refundUUID': '1' * 32,
            'refundPaymentStatus': 'unknown',
            'refundCreatedAt': '2017-10-10T16:40:00+00:00',
        }))

        data = async_urlconf_client.get('/ru/train-purchase-backoffice/pending-refund-payments/', {
            'minutesPassed': 5
        }).data
        assert data['total'] == 2
