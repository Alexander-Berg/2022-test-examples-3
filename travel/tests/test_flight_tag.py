# -*- coding: utf-8 -*-
from datetime import datetime

import pytest

from travel.avia.ticket_daemon_api.jsonrpc.lib.flights import IATAFlight


@pytest.mark.parametrize(
    ('number', 'departure_time', 'expected_tag'), [
        ('SU 123', datetime(2020, 12, 31, 10, 50, 00), u'2012311050SU123'),
        ('FV123', datetime(2019, 1, 1, 23, 59, 59), u'1901012359FV123'),
        (u'ВГ 11', datetime(2018, 2, 8, 1, 2, 3), u'1802080102ВГ11'),
        (u'СУ 22', None, u'СУ22'),
    ]
)
def test_flight_tag(number, departure_time, expected_tag):
    assert IATAFlight.make_flight_tag(local_departure=departure_time, number=number) == expected_tag
