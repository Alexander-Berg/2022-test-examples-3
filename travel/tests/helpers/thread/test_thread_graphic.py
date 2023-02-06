import unittest

from travel.rasp.touch.modelBuilders.thread.thread_graphic import (
    _get_transfer_segments, _group_station_by_segments, create_scheme, create_station_node, create_collapsed_node
)


class TestGetTransferSegments(unittest.TestCase):
    def test_start_is_transfer(self):
        raw_stations = [(0, False), (1, True), (2, False), (3, False), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]

        segments = _get_transfer_segments(stations, 1, 3)

        assert len(segments) == 1
        assert segments[0][0] == 1
        assert segments[0][1] == 3

    def test_end_is_transfer(self):
        raw_stations = [(0, False), (1, False), (2, False), (3, True), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]

        segments = _get_transfer_segments(stations, 1, 3)

        assert len(segments) == 1
        assert segments[0][0] == 1
        assert segments[0][1] == 3

    def test_transfer_in_user_path(self):
        raw_stations = [(0, False), (1, False), (2, True), (3, False), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]

        segments = _get_transfer_segments(stations, 1, 3)

        assert len(segments) == 2
        assert segments[0][0] == 1
        assert segments[0][1] == 2

        assert segments[1][0] == 2
        assert segments[1][1] == 3

    def test__many_transfer_in_user_path(self):
        raw_stations = [(0, False), (1, False), (2, True), (3, False), (4, True), (5, True), (6, False), (7, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]

        segments = _get_transfer_segments(stations, 1, 6)

        assert len(segments) == 4
        assert segments[0][0] == 1
        assert segments[0][1] == 2

        assert segments[1][0] == 2
        assert segments[1][1] == 4

        assert segments[2][0] == 4
        assert segments[2][1] == 5

        assert segments[3][0] == 5
        assert segments[3][1] == 6

    def test__without_transfer_in_user_path(self):
        raw_stations = [(0, False), (1, False), (3, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]

        segments = _get_transfer_segments(stations, 1, 3)

        assert len(segments) == 1
        assert segments[0][0] == 1
        assert segments[0][1] == 3


class TestGroupStationBySegments(unittest.TestCase):
    def test_start_and_end_in_path(self):
        user_start = 1
        user_end = 3
        stations = [(0, False), (1, False), (2, False), (3, False), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in stations]
        segments = _get_transfer_segments(stations, user_start, user_end)

        actual = _group_station_by_segments(segments, stations, user_start, user_end)

        assert actual == [create_scheme([
            create_station_node(stations[0], False, 'departure'),
            create_collapsed_node([], False),
            create_station_node(stations[1], True, 'departure'),
            create_collapsed_node([stations[2]], True),
            create_station_node(stations[3], True, 'arrival'),
            create_collapsed_node([], False),
            create_station_node(stations[4], False, 'arrival'),
        ])]

    def test_start_in_path(self):
        user_start = 1
        user_end = 4
        raw_stations = [(0, False), (1, False), (2, False), (3, False), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]
        segments = _get_transfer_segments(stations, user_start, user_end)

        actual = _group_station_by_segments(segments, stations, user_start, user_end)

        assert actual == [create_scheme([
            create_station_node(stations[0], False, 'departure'),
            create_collapsed_node([], False),
            create_station_node(stations[1], True, 'departure'),
            create_collapsed_node(stations[2:4], True),
            create_station_node(stations[4], True, 'arrival'),
        ])]

    def test_end_in_path(self):
        user_start = 0
        user_end = 3
        raw_stations = [(0, False), (1, False), (2, False), (3, False), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]
        segments = _get_transfer_segments(stations, user_start, user_end)

        actual = _group_station_by_segments(segments, stations, user_start, user_end)

        assert actual == [create_scheme([
            create_station_node(stations[0], True, 'departure'),
            create_collapsed_node(stations[1:3], True),
            create_station_node(stations[3], True, 'arrival'),
            create_collapsed_node([], False),
            create_station_node(stations[4], False, 'arrival'),
        ])]

    def test_user_path_equal_path(self):
        user_start = 0
        user_end = 4
        raw_stations = [(0, False), (1, False), (2, False), (3, False), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]
        segments = _get_transfer_segments(stations, user_start, user_end)

        actual = _group_station_by_segments(segments, stations, user_start, user_end)

        assert actual == [create_scheme([
            create_station_node(stations[0], True, 'departure'),
            create_collapsed_node(stations[1:-1], True),
            create_station_node(stations[4], True, 'arrival'),
        ])]

    def test_with_transfer(self):
        user_start = 1
        user_end = 3
        raw_stations = [(0, False), (1, False), (2, True), (3, False), (4, False)]

        stations = [FakeStation(s[0], s[1]) for s in raw_stations]
        segments = _get_transfer_segments(stations, user_start, user_end)

        actual = _group_station_by_segments(segments, stations, user_start, user_end)

        assert actual == [create_scheme([
            create_station_node(stations[0], False, 'departure'),
            create_collapsed_node([], False),
            create_station_node(stations[1], True, 'departure'),
            create_collapsed_node([], True),
            create_station_node(stations[2], True, 'arrival'),
        ]), create_scheme([
            create_station_node(stations[2], True, 'departure'),
            create_collapsed_node([], True),
            create_station_node(stations[3], True, 'arrival'),
            create_collapsed_node([], False),
            create_station_node(stations[4], False, 'arrival'),
        ])]


class FakeStation(object):
    def __init__(self, name, is_combined):
        self.name = name
        self.is_combined = is_combined

    def __eq__(self, other):
        if isinstance(other, FakeStation):
            return self.name == other.name and self.is_combined == other.is_combined
        return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __repr__(self):
        return str({
            'name': self.name,
            'is_combined': self.is_combined
        })

    def __str__(self):
        return str({
            'name': self.name,
            'is_combined': self.is_combined
        })
