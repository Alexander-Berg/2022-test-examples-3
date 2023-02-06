# coding: utf-8

from travel.rasp.admin.scripts.schedule.utils.times_approximation import fill_middle_times
from tester.testcase import TestCase


class Stop(object):
    def __init__(self, arrival, departure):
        self.arrival = arrival
        self.departure = departure


class TestFillMiddleTimes(TestCase):
    def test_fill_middle_times_none_x(self):
        stops = [
            Stop(None, 0),
            Stop(None, 10),
            Stop(19, 20),
            Stop(30, None)
        ]

        fill_middle_times(stops, ignore_bad_duration=True)

        assert stops[1].arrival is not None
        assert stops[1].departure is not None

    def test_fill_middle_times_none_none(self):
        stops = [
            Stop(None, 0),
            Stop(None, None),
            Stop(19, 20),
            Stop(30, None)
        ]

        fill_middle_times(stops, ignore_bad_duration=True)

        assert stops[1].arrival is not None
        assert stops[1].departure is not None

    def test_fill_middle_times_x_none(self):
        stops = [
            Stop(None, 0),
            Stop(10, None),
            Stop(19, 20),
            Stop(30, None)
        ]

        fill_middle_times(stops, ignore_bad_duration=True)

        assert stops[1].arrival is not None
        assert stops[1].departure is not None

    def test_fill_middle_times_less(self):
        stops = [
            Stop(None, 0),
            Stop(None, None),
            Stop(0, 1),
            Stop(30, None)
        ]

        fill_middle_times(stops, ignore_bad_duration=True)

        for stop in stops[1:-1]:
            assert stop.arrival is not None
            assert stop.departure is not None
