from datetime import date

import pytest

from mail.ciao.ciao.api.schemas.megamind.parsers.sys_weekdays import parse_sys_weekdays


@pytest.fixture(autouse=True)
def mock_today(mocker, today):
    # Mocking "today" directly causes:
    #   TypeError: can't set attributes of built-in/extension type 'datetime.date'
    mock_date = mocker.patch('mail.ciao.ciao.api.schemas.megamind.parsers.sys_weekdays.date')
    mock_date.today.return_value = today
    mock_date.side_effect = lambda *args, **kwargs: date(*args, **kwargs)


@pytest.mark.parametrize('today,input_value,expected_output', (
    (
        date(2020, 2, 17),  # Monday
        '{"repeat":false,"weekdays":[2]}',  # Tuesday
        date(2020, 2, 18),
    ),
    (
        date(2020, 2, 19),  # Wednesday
        '{"repeat":false,"weekdays":[2]}',  # Tuesday
        date(2020, 2, 25),
    ),
))
def test_parse_sys_weekdays(input_value, expected_output):
    assert parse_sys_weekdays(input_value) == expected_output
