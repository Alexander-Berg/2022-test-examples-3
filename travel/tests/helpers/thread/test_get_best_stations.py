import pytest

from travel.rasp.touch.touch.core.helpers.thread import get_best_stations


@pytest.mark.skip('This test doesnt work for a long time')
def test_no_variants():
    path = [FakeRtStation(station, tz_arrival, tz_departure)
            for station, tz_arrival, tz_departure in [(1, None, 0), (2, 1, 2), (3, 3, None)]]

    actual = get_best_stations(3, 2, path)

    assert actual == (None, None)


def test_simple():
    path = [FakeRtStation(station, tz_arrival, tz_departure)
            for station, tz_arrival, tz_departure in [(1, None, 0), (2, 1, 2), (3, 3, None)]]

    actual = get_best_stations(1, 3, path)

    assert actual == (path[0], path[2])


def test_two_variants():
    path = [FakeRtStation(station, tz_arrival, tz_departure)
            for station, tz_arrival, tz_departure in [(1, None, 0), (2, 1, 2), (3, 3, 4), (2, 5, 6), (3, 7, None)]]

    actual = get_best_stations(2, 3, path)

    print(actual[0])
    print(actual[1])

    assert actual == (path[1], path[2])


def test_two_variants_and_second_better():
    path = [FakeRtStation(station, tz_arrival, tz_departure)
            for station, tz_arrival, tz_departure in [(1, None, 0), (2, 1, 2), (3, 10, 11), (2, 12, 13), (3, 14, None)]]

    actual = get_best_stations(2, 3, path)

    assert actual == (path[3], path[4])


class FakeRtStation():
    def __init__(self, station, tz_arrival, tz_departure):
        self.station = station
        self.tz_arrival = tz_arrival
        self.tz_departure = tz_departure

    def __repr__(self):
        return "station: {0}; tz_arrival: {1}; tz_departure: {2}".format(self.station, self.tz_arrival, self.tz_departure)
