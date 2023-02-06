# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from datetime import datetime

import pytest

from travel.avia.ticket_daemon.ticket_daemon.lib.utils import fix_flight_number
from travel.avia.ticket_daemon.ticket_daemon.lib.utils import parse_datetime_without_seconds


@pytest.mark.parametrize('flight_number,expected_value', [
    ('U6 1', 'U6 1'),
    ('U6 1Д', 'U6 1'),
    ('U6 U61234Д', 'U6 1234'),
    ('F B - FB123Д', 'FB 123'),
    ('РУ 12D', 'РУ 12'),
    ('ФF 12D', 'ФФ 12'),
    ('SU 12345', 'SU 1234'),
    ('СУ 12345', 'СУ 1234'),
    ('UT 535D', 'UT 535'),
    ('ZF 3231a', 'ZF 3231'),
    ('zf 3231a', 'ZF 3231'),
    ('TRA SHcompa- -ny1 233number', None),
    ('99 123', None),
    ('1f 123', '1F 123'),
    ('1я 123', '1Я 123'),
    ('U6 0001', 'U6 1'),
    ('1f 0123', '1F 123'),
    ('1я 0123', '1Я 123'),
    ('SU 0036', 'SU 36'),
])
def test_fix_flight_number(flight_number, expected_value):
    assert fix_flight_number(flight_number, is_charter=False) == expected_value


@pytest.mark.parametrize('flight_number,expected_value', [
    ('U6 1', 'U6 1'),
    ('U6 1Д', 'U6 1'),
    ('U6 U61234Д', 'U6 1234'),
    ('F B - FB123Д', 'FB 123'),
    ('РУ 12D', 'РУ 12'),
    ('ФF 12D', 'ФФ 12'),
    ('UT 535D', 'UT 535'),
    ('ZF 3231a', 'ZF 3231'),
    ('SU 12345', 'SU 12345'),
    ('СУ 123456GR', 'СУ 123456'),
    ('YC 939401', 'YC 939401'),
    ('yc 939401', 'YC 939401'),
    ('TRA SHcompa- -ny1 233number', None),
    ('99 123', None),
    ('1f 123', '1F 123'),
    ('1я 123', '1Я 123'),
    ('U6 0001', 'U6 1'),
    ('1f 0123', '1F 123'),
    ('1я 0123', '1Я 123'),
])
def test_fix_flight_number_charter(flight_number, expected_value):
    assert fix_flight_number(flight_number, is_charter=True) == expected_value


def test_parse_datetime_without_seconds():
    actual = parse_datetime_without_seconds('2018-01-21T13:50:59')
    expected = datetime.strptime('2018-01-21T13:50:00', '%Y-%m-%dT%H:%M:%S')

    assert actual == expected
