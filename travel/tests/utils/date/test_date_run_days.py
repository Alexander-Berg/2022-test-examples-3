# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, time

import mock
import pytest
import pytz
from freezegun import freeze_time
from hamcrest import has_entries, assert_that, is_not

from common.tester.factories import create_rthread_segment, create_thread
from common.tests.utils import has_route_search

from travel.rasp.library.python.common23.date.date_const import MSK_TIMEZONE
from travel.rasp.library.python.common23.date.run_mask import RunMask
from travel.rasp.library.python.common23.models.core.schedule.run_days import calculate_run_days, thread_run_days
from travel.rasp.library.python.common23.models.core.schedule.rthread_type import RThreadType

# TODO: #py23remove
"""
Эти тесты тестируют travel.rasp.library.python.common23.models.core.schedule.run_days
Их пока нельзя перенести, т.к. они зависят от route_search.
Но свою функцию они выполняют.
"""

@freeze_time('2016-03-01')
@pytest.mark.dbuser
@pytest.mark.parametrize("thread_type", [
    RThreadType.BASIC_ID,
    RThreadType.INTERVAL_ID
])
@has_route_search
def test_calculate_run_days(thread_type):
    """
    Так как мы должны вернуть дни хождения на "сегодня" - 30 (days_ago=30), то, так как
    в феврале 2016-го года было 29 дней, мы попадаем на январь и возвращаем
    так же и дни хождения в январе.
    """
    january_mask = '1' * 31
    february_mask = '10101010101011111111111111100'
    march_mask = '0' * 31
    segment = create_rthread_segment(thread=create_thread(type=thread_type))
    m_shifted = mock.Mock(return_value=january_mask + february_mask + '11' + march_mask + '1' * 279)
    with mock.patch.object(RunMask, 'shifted', m_shifted):
        result = calculate_run_days(segment, days_ago=30)
        m_shifted.assert_called_once_with(0)
        assert_that(result, has_entries(**{
            '2016': has_entries(**{
                '1': list(map(int, january_mask)),
                '2': list(map(int, february_mask)),
            }),
        }))
        assert_that(result, has_entries(**{
            '2016': is_not(has_entries(**{
                '3': list(map(int, march_mask))
            })),
        }))


@freeze_time('2016-05-15')
@pytest.mark.dbuser
@has_route_search
def test_calculate_run_days_result_timezone():
    """
    Проверяем смещение календаря при передаче параметра result_timezone.
    """
    msk_mask = '0000000001111111111000000000000'
    ksn_mask = '0000000000111111111100000000000'
    ksn_timezone = pytz.timezone('Asia/Krasnoyarsk')

    thread = create_thread(
        tz_start_time=time(22),
        year_days=RunMask.range(datetime(2016, 5, 10), datetime(2016, 5, 20)),
        time_zone=MSK_TIMEZONE
    )
    segment = create_rthread_segment(thread=thread)

    result = calculate_run_days(segment)
    assert_that(result, has_entries(**{
        '2016': has_entries(**{
            '5': list(map(int, msk_mask)),
        }),
    }))

    result = calculate_run_days(segment, result_timezone=ksn_timezone)
    assert_that(result, has_entries(**{
        '2016': has_entries(**{
            '5': list(map(int, ksn_mask)),
        }),
    }))


@freeze_time('2018-11-20')
@pytest.mark.dbuser
@has_route_search
def test_make_run_days():
    thread = create_thread(
        tz_start_time=time(12),
        year_days=[datetime(2018, 10, 20), datetime(2018, 11, 10), datetime(2018, 11, 15), datetime(2018, 12, 3)]
    )

    result = thread_run_days(thread, shift=0, days_ago=0)
    assert_that(result, has_entries(**{
        '2018': {
            '11': [0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            '12': [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }
    }))

    result = thread_run_days(thread, shift=3, days_ago=30)
    assert_that(result, has_entries(**{
        '2018': {
            '10': [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0],
            '11': [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            '12': [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }
    }))
