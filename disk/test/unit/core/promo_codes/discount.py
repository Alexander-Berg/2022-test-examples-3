# -*- coding: utf-8 -*-
import datetime
import time

from nose_parameterized import parameterized

from mpfs.common.static.tags.billing import (
    PRIMARY_2018_DISCOUNT_30,
    PRIMARY_2018_DISCOUNT_20,
    PRIMARY_2018_DISCOUNT_10,
)
from mpfs.core.promo_codes.logic.discount import DiscountTemplate, Discount
from test.base import time_machine
from test.unit.base import NoDBTestCase


class DiscountTestCase(NoDBTestCase):

    @parameterized.expand([
        (PRIMARY_2018_DISCOUNT_30, True, None, PRIMARY_2018_DISCOUNT_20, True, None),
        (PRIMARY_2018_DISCOUNT_30, True, None, PRIMARY_2018_DISCOUNT_10, True, None),
        (PRIMARY_2018_DISCOUNT_20, True, None, PRIMARY_2018_DISCOUNT_10, True, None),
        (PRIMARY_2018_DISCOUNT_20, True, None, PRIMARY_2018_DISCOUNT_10, False, None),
        (PRIMARY_2018_DISCOUNT_10, False, None, PRIMARY_2018_DISCOUNT_10, True, None),
        (PRIMARY_2018_DISCOUNT_10, False, None, PRIMARY_2018_DISCOUNT_10, True, datetime.timedelta(seconds=10)),
        (PRIMARY_2018_DISCOUNT_10, False, datetime.timedelta(seconds=10), PRIMARY_2018_DISCOUNT_10, True, None),
        (PRIMARY_2018_DISCOUNT_10, False, datetime.timedelta(seconds=10), PRIMARY_2018_DISCOUNT_10, False, datetime.timedelta(seconds=20)),
        (PRIMARY_2018_DISCOUNT_10, False, datetime.timedelta(seconds=20), PRIMARY_2018_DISCOUNT_10, True, datetime.timedelta(seconds=10)),
        (PRIMARY_2018_DISCOUNT_20, False, datetime.timedelta(seconds=20), PRIMARY_2018_DISCOUNT_10, False, datetime.timedelta(seconds=10)),
    ])
    def test_different_lines(self, line_1, disp_1, td_1, line_2, disp_2, td_2):
        s_1 = Discount.create('1', 0, DiscountTemplate.create(line_1, '', disp_1, td_1))
        s_2 = Discount.create('1', 0, DiscountTemplate.create(line_2, '', disp_2, td_2))
        assert s_1 < s_2
        assert not (s_2 < s_1)

    @parameterized.expand([
        (PRIMARY_2018_DISCOUNT_10, PRIMARY_2018_DISCOUNT_10),
        (PRIMARY_2018_DISCOUNT_20, PRIMARY_2018_DISCOUNT_20),
        (PRIMARY_2018_DISCOUNT_30, PRIMARY_2018_DISCOUNT_30),
    ])
    def test_similar(self, line_1, line_2):
        s_1 = Discount.create('1', 0, DiscountTemplate.create(line_1, ''))
        s_2 = Discount.create('1', 0, DiscountTemplate.create(line_2, ''))
        assert not (s_1 < s_2)
        assert not (s_2 < s_1)

    def test_sort(self):
        offers = [
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_30, '', False, None)), 0),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_20, '', False, None)), 4),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, '', False, None)), 8),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_30, '', True, None)), 3),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_20, '', True, None)), 7),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, '', True, None)), 11),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_30, '', False, datetime.timedelta(seconds=10))), 1),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_20, '', False, datetime.timedelta(seconds=10))), 5),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, '', False, datetime.timedelta(seconds=10))), 9),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_30, '', False, datetime.timedelta(seconds=15))), 2),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_20, '', False, datetime.timedelta(seconds=20))), 6),
            (Discount.create('1', 0, DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, '', False, datetime.timedelta(seconds=30))), 10),
        ]
        assert [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11] == [x[1] for x in sorted(offers)]

    def test_period_and_end_datetime_are_equal(self):
        cur_time = int(time.time())
        cur_datetime = datetime.datetime.fromtimestamp(cur_time)
        with time_machine(cur_datetime):
            ud_1 = Discount.create(
                '1', cur_time, DiscountTemplate.create(
                    PRIMARY_2018_DISCOUNT_30, '', disposable=False, period_timedelta=datetime.timedelta(seconds=10)))
            ud_2 = Discount.create(
                '2', cur_time, DiscountTemplate.create(
                    PRIMARY_2018_DISCOUNT_30, '', disposable=False, end_datetime=cur_datetime + datetime.timedelta(seconds=10)))
        assert ud_1.end_datetime == ud_2.end_datetime
