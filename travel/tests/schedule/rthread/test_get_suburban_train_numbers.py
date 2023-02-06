# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, contains_inanyorder

from travel.rasp.library.python.common23.models.core.schedule.train_purchase_number import TrainPurchaseNumber
from travel.rasp.library.python.common23.tester.factories import create_thread


@pytest.mark.dbuser
def test_many_numbers():
    rt = create_thread()
    rt2 = create_thread()
    rt3 = create_thread()
    threads = [rt, rt2]

    TrainPurchaseNumber.objects.create(thread=rt, number='9412')
    TrainPurchaseNumber.objects.create(thread=rt2, number='9413')
    TrainPurchaseNumber.objects.create(thread=rt, number='9414')
    TrainPurchaseNumber.objects.create(thread=rt3, number='9415')

    result = TrainPurchaseNumber.get_train_purchase_numbers(threads)
    assert_that([v for row in result.values() for v in row], contains_inanyorder('9412', '9413', '9414'))
    assert_that(result[rt.id], contains_inanyorder('9412', '9414'))
