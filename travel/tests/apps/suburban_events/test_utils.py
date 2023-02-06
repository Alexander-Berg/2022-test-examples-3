# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

from hamcrest import assert_that, has_properties

from common.apps.suburban_events.utils import (
    ThreadKey, ThreadStationKey, ThreadEventsTypeCodes, ClockDirection, prefix_for_dict_keys
)


def test_thread_key_from_str():
    str_key = '6322__9608607___2017-11-22T09:40:00___None___None'
    thread_key = ThreadKey.from_str(str_key)
    assert_that(thread_key,
                has_properties({
                    'thread_key': '6322__9608607',
                    'thread_start_date': datetime(2017, 11, 22, 9, 40),
                    'thread_type': ThreadEventsTypeCodes.SUBURBAN,
                    'clock_direction': None
                }))
    assert thread_key.to_str() == str_key

    thread_key = ThreadKey.from_str('МЦК__9608600___2017-11-22T12:30:00___mczk___0')
    assert_that(thread_key,
                has_properties({
                    'thread_key': 'МЦК__9608600',
                    'thread_start_date': datetime(2017, 11, 22, 12, 30),
                    'thread_type': ThreadEventsTypeCodes.MCZK,
                    'clock_direction': ClockDirection.C_CLOCK_WISE
                }))

    str_key = 'МЦК__9608600___2017-11-22T12:30:00___mczk___1'
    thread_key = ThreadKey.from_str(str_key)
    assert_that(thread_key,
                has_properties({
                    'clock_direction': ClockDirection.CLOCK_WISE
                }))
    assert thread_key.to_str() == str_key


def test_thread_station_key_from_str():
    str_key = '6322__9608607___2017-11-22T09:40:00___9610865___10___15___None___None'
    thread_key = ThreadStationKey.from_str(str_key)
    assert_that(thread_key,
                has_properties({
                    'thread_key': '6322__9608607',
                    'thread_start_date': datetime(2017, 11, 22, 9, 40),
                    'station_key': '9610865',
                    'arrival': 10,
                    'departure': 15,
                    'thread_type': ThreadEventsTypeCodes.SUBURBAN,
                    'clock_direction': None
                }))
    assert thread_key.to_str() == str_key

    thread_key = ThreadStationKey.from_str('МЦК__9608600___2017-11-22T12:30:00___9610865___10___None___mczk___0')
    assert_that(thread_key,
                has_properties({
                    'thread_key': 'МЦК__9608600',
                    'arrival': 10,
                    'departure': None,
                    'thread_start_date': datetime(2017, 11, 22, 12, 30),
                    'thread_type': ThreadEventsTypeCodes.MCZK,
                    'clock_direction': ClockDirection.C_CLOCK_WISE
                }))

    str_key = 'МЦК__9608600___2017-11-22T12:30:00___9610865___None___15___mczk___1'
    thread_key = ThreadStationKey.from_str(str_key)
    assert_that(thread_key,
                has_properties({
                    'arrival': None,
                    'departure': 15,
                    'clock_direction': ClockDirection.CLOCK_WISE
                }))
    assert thread_key.to_str() == str_key


def test_prefix_for_dict_keys():
    assert prefix_for_dict_keys({'a': 1, 'b': 2}, 'key.') == {'key.a': 1, 'key.b': 2}
