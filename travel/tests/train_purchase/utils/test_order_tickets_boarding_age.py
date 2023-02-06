# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import pytest

from travel.rasp.train_api.train_purchase.utils.order_tickets import _calculate_boarding_age_by_dates


@pytest.mark.parametrize('departure_date, birth_date, expected', (
    (date(2018, 4, 14), date(2000, 5, 15), 17),
    (date(2018, 4, 15), date(2000, 5, 15), 17),
    (date(2018, 4, 16), date(2000, 5, 15), 17),
    (date(2018, 5, 14), date(2000, 5, 15), 17),
    (date(2018, 5, 15), date(2000, 5, 15), 18),
    (date(2018, 5, 16), date(2000, 5, 15), 18),
    (date(2018, 6, 14), date(2000, 5, 15), 18),
    (date(2018, 6, 15), date(2000, 5, 15), 18),
    (date(2018, 6, 16), date(2000, 5, 15), 18),
))
def test_calculate_boarding_age_by_dates(departure_date, birth_date, expected):
    assert _calculate_boarding_age_by_dates(departure_date, birth_date) == expected
