import pytest
import datetime
from mail.catdog.catdog.src import logger


@pytest.mark.parametrize(('value', 'escaped'), [
    ('', ''),
    ('abc', 'abc'),
    ('\t \n \r \0 \\', '\\t \\n \\r \\0 \\\\'),
    ('測試', '測試'),
    (datetime.datetime(day=1, month=1, year=1970, hour=0,
                       minute=0, second=0, tzinfo=datetime.timezone.utc),
        '1970-01-01 00:00:00+00:00'),
    (42, '42'),
    (1.5, '1.5'),
    (None, '')
])
def test_escape_value(value, escaped):
    assert escaped == logger.escape_value(value)
