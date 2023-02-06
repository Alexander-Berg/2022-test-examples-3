from hamcrest import assert_that, equal_to

from market.idx.datacamp.routines.lib.blueprints.force_mine_progress import MineStats, get_stats_from_request


class GolovanRequestMock(object):
    def __init__(self, data, key):
        self.data = data
        self.key = key

    def __iter__(self):
        for ts, value in self.data:
            yield ts, {self.key: value}


def test_normal_behavior():
    stats = MineStats()
    stats.row_count = 10
    data = [(1, 1), (2, 1), (3, 1)]

    get_stats_from_request(stats, GolovanRequestMock(data, 'key'), 'key')

    expected = MineStats(10, 3, 7, 1, 7)
    assert_that(stats.dumps(), equal_to(expected.dumps()))


def test_mined_more():
    stats = MineStats()
    stats.row_count = 10
    data = [(1, 10), (2, 10), (3, 10)]

    get_stats_from_request(stats, GolovanRequestMock(data, 'key'), 'key')

    expected = MineStats(10, 30, 0, 0, 0)
    assert_that(stats.dumps(), equal_to(expected.dumps()))


def test_mined_nothing():
    stats = MineStats()
    stats.row_count = 10
    data = []

    get_stats_from_request(stats, GolovanRequestMock(data, 'key'), 'key')

    expected = MineStats(10, 0, 10, 0, 0)
    assert_that(stats.dumps(), equal_to(expected.dumps()))


def test_mined_nothing2():
    stats = MineStats()
    stats.row_count = 10
    data = [(1, 0), (2, 0), (3, 0)]

    get_stats_from_request(stats, GolovanRequestMock(data, 'key'), 'key')

    expected = MineStats(10, 0, 10, 0, 0)
    assert_that(stats.dumps(), equal_to(expected.dumps()))


def test_none():
    stats = MineStats()
    stats.row_count = 10
    data = [(1, 1), (2, None), (3, None), (4, None)]

    get_stats_from_request(stats, GolovanRequestMock(data, 'key'), 'key')

    expected = MineStats(10, 1, 9, 0.25, 36)
    assert_that(stats.dumps(), equal_to(expected.dumps()))
