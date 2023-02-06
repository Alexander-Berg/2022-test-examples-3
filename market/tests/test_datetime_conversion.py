# coding=utf-8
import datetime

import pytest
import pytz

from lib.blueprints.cubes_arrival import convert_datetime_to_utc
import logging

log = logging.getLogger()


# Не повторяйте моих ошибок и не используйте вот такую запись
# datetime.datetime(2020, 5, 24, 0, 0, 0, tzinfo=pytz.timezone('Europe/Moscow'))
# И .replace(tzinfo=pytz.timezone('Europe/Moscow')) тоже
# pytz такое не любит и добавляет странные оффсеты в минутах, для Москвы считает как +2:30 а не +3, например
@pytest.mark.parametrize("original_datetime, expected_datetime",
                         [
                             (
                                 # original_datetime
                                 datetime.datetime(2020, 5, 24, 0, 0, 0),
                                 # expected_datetime
                                 datetime.datetime(2020, 5, 23, 21, 0, 0, tzinfo=pytz.utc),
                             ),
                             (
                                 # original_datetime
                                 pytz.timezone('Europe/Moscow').localize(datetime.datetime(2020, 5, 24, 0, 0, 0)),
                                 # expected_datetime
                                 datetime.datetime(2020, 5, 23, 21, 0, 0, tzinfo=pytz.utc),
                             ),
                             (
                                 # original_datetime
                                 pytz.timezone('Asia/Omsk').localize(datetime.datetime(2020, 5, 24, 0, 0, 0)),
                                 # expected_datetime
                                 datetime.datetime(2020, 5, 23, 18, 0, 0, tzinfo=pytz.utc),
                             ),
                             (
                                 # original_datetime
                                 datetime.datetime(2020, 5, 24, 0, 0, 0, tzinfo=pytz.utc),
                                 # expected_datetime
                                 datetime.datetime(2020, 5, 24, 0, 0, 0, tzinfo=pytz.utc),
                             ),
                         ]
                         )
def test_convert_datetime_to_utc(original_datetime, expected_datetime):
    log.debug('Original %s', original_datetime)
    result = convert_datetime_to_utc(original_datetime)
    log.debug('Converted %s', result)
    assert result == expected_datetime
