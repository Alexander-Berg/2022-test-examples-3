# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest
from bson import ObjectId

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_purchase.core.factories import RefundBlankFactory
from travel.rasp.train_api.train_purchase.core.models import RefundBlank
from travel.rasp.train_api.train_purchase.tasks import collect_refund_blanks


@pytest.mark.mongouser
@replace_now('2001-01-01 00:15:00')
@replace_setting('REFUND_BLANKS_TTL', timedelta(minutes=15))
def test_collect_outdated_refund_blanks():
    blank_to_remove = RefundBlankFactory(id=ObjectId.from_datetime(MSK_TZ.localize(datetime(2001, 1, 1, 0, 0, 0))))
    blank_to_stay = RefundBlankFactory(id=ObjectId.from_datetime(MSK_TZ.localize(datetime(2001, 1, 1, 0, 0, 1))))
    collect_refund_blanks.collect_outdated_refund_blanks()

    assert RefundBlank.objects(pk=blank_to_remove.id).first() is None
    assert RefundBlank.objects(pk=blank_to_stay.id).get() is not None
