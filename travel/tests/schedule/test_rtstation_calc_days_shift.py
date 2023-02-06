# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
from datetime import time, datetime

import pytz

from travel.rasp.library.python.common23.tester.factories import create_thread
from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.tester.utils.datetime import replace_now


TEST_DATE = datetime(2015, 11, 10)


class TestRTStationCalcDayShift(TestCase):
    @replace_now(TEST_DATE)
    def test_shift(self):
        thread = create_thread(
            time_zone='Europe/Moscow',
            tz_start_time=time(13, 20),
        )

        first_rts = thread.path[0]
        assert 0 == first_rts.calc_days_shift(event='departure', start_date=TEST_DATE,
                                              event_tz=pytz.timezone('Asia/Yekaterinburg'))
        assert -1 == first_rts.calc_days_shift(event='departure', start_date=TEST_DATE,
                                               event_tz=pytz.timezone('Pacific/Samoa'))
        assert 1 == first_rts.calc_days_shift(event='departure', start_date=TEST_DATE,
                                              event_tz=pytz.timezone('Pacific/Kiritimati'))
