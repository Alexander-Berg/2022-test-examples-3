from datetime import date, datetime

import pytest
import pytz

from mail.ciao.ciao.utils.format import (
    _MONTHS, _MONTHS_GENITIVE, format_date, format_datetime, format_month, format_time
)


@pytest.fixture
def dt():
    return datetime(2020, 2, 17, 10, 35, 9)


def test_format_time(dt):
    assert format_time(dt) == dt.strftime('%H:%M')


def test_format_time_with_timezone(dt):
    tz = pytz.timezone('Europe/Moscow')
    assert format_time(tz.localize(dt)) == dt.strftime('%H:%M')


@pytest.mark.parametrize('date_,today,expected', (
    pytest.param(
        date(2020, 2, 17),
        date(2020, 3, 15),
        '17 февраля',
        id='same_year',
    ),
    pytest.param(
        date(2020, 2, 17),
        date(2007, 3, 15),
        '17 февраля 2020 года',
        id='different_year',
    ),
))
def test_format_date(date_, today, expected):
    assert format_date(date_, today) == expected


@pytest.mark.parametrize('genitive', (True, False))
def test_format_month(genitive, randn):
    num = randn(min=1, max=12)
    title = _MONTHS_GENITIVE[num] if genitive else _MONTHS[num]
    assert format_month(num, genitive=genitive) == title


class TestFormatDateTime:
    @pytest.fixture(autouse=True)
    def format_time_mock(self, mocker):
        return mocker.patch(
            'mail.ciao.ciao.utils.format.format_time',
            mocker.Mock(return_value='time')
        )

    @pytest.fixture(autouse=True)
    def format_date_mock(self, mocker):
        return mocker.patch(
            'mail.ciao.ciao.utils.format.format_date',
            mocker.Mock(return_value='date')
        )

    @pytest.fixture
    def today(self, mocker):
        return mocker.Mock()

    def test_result(self, dt, today, format_time_mock, format_date_mock):
        assert format_datetime(dt, today) == f'{format_date_mock.return_value} {format_time_mock.return_value}'

    def test_format_time_call(self, dt, today, format_time_mock):
        format_datetime(dt, today)
        format_time_mock.assert_called_once_with(dt)

    def test_format_date_call(self, dt, today, format_date_mock):
        format_datetime(dt, today)
        format_date_mock.assert_called_once_with(dt, today)
