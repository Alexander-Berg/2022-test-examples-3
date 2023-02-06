# coding: utf-8

from datetime import datetime

import pytest
import pytz

from travel.rasp.morda_backend.morda_backend.tariffs.static.partners.helpers import get_number_of_days_to_departure


@pytest.mark.parametrize('now, now_tz, departure, departure_tz, expected', (
    (datetime(2016, 9, 6, 16), 'Europe/Moscow', datetime(2016, 9, 6, 23), 'Europe/Moscow', 0),
    (datetime(2016, 9, 6, 16), 'Europe/Moscow', datetime(2016, 9, 7, 23), 'Europe/Moscow', 1),
    (datetime(2016, 9, 6, 16), 'Europe/Moscow', datetime(2016, 9, 7, 1), 'Asia/Yekaterinburg', 1),
    (datetime(2016, 9, 6, 5), 'Asia/Yekaterinburg', datetime(2016, 9, 6, 16), 'Europe/Moscow', 0),
    (datetime(2016, 9, 6, 1), 'Asia/Yekaterinburg', datetime(2016, 9, 6, 16), 'Europe/Moscow', 1),
))
def test_get_number_of_days_to_departure(now, now_tz, departure, departure_tz, expected):
    now_aware = pytz.timezone(now_tz).localize(now)
    departure_aware = pytz.timezone(departure_tz).localize(departure)
    assert get_number_of_days_to_departure(now_aware, departure_aware) == expected
