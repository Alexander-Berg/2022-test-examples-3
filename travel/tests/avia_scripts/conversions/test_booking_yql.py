# coding=utf-8
from __future__ import unicode_literals, absolute_import, print_function

from collections import OrderedDict
from datetime import date

from travel.avia.admin.avia_scripts.conversion.booking_yql import build_booking_data_yql
from travel.avia.admin.avia_scripts.conversion.intervals import DateInterval


def _normalize(yql):
    return ' '.join(yql.strip().split())


def test_build_booking_yql():
    expected = """
        use hahn;
        SELECT
            label
        FROM `//cpa/orders/path`
        WHERE
            status = 'confirmed'
            AND (
                (
                    billing_order_id = 42
                    AND created_at >= 1593550800
                    AND created_at < 1594242000
                )
                OR (
                    billing_order_id = 100500
                    AND created_at >= 1571518800
                    AND created_at < 1572210000
                )
            )
    """

    # use OrderedDict for deterministic output
    interval_by_billing_order_id = OrderedDict()
    interval_by_billing_order_id[42] = DateInterval(date(2020, 7, 1), date(2020, 7, 8))
    interval_by_billing_order_id[100500] = DateInterval(date(2019, 10, 20), date(2019, 10, 27))

    yql = build_booking_data_yql('//cpa/orders/path', interval_by_billing_order_id)
    assert _normalize(yql) == _normalize(expected)
