# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
from travel.rasp.library.python.common23.tester.factories import create_thread, create_station
from travel.rasp.library.python.common23.tester.testcase import TestCase


class TestRTStationIsNoStop(TestCase):
    def test_is_no_stop(self):
        station_from, station_to, station_mid = create_station(), create_station(), create_station()
        thread = create_thread(
            schedule_v1=[
                [None, 0, station_from],
                [1, 1, station_mid],
                [5, None, station_to],
            ],
        )
        path = thread.path
        assert path[0].is_no_stop() is False
        assert path[1].is_no_stop() is True
        assert path[2].is_no_stop() is False
