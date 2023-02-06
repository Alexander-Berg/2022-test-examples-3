# coding=utf-8
import pytest

from travel.avia.admin.avia_scripts.conversion.intervals import DateInterval, join_intervals


@pytest.mark.parametrize('intervals, expected', (
    (None, None),
    ([], []),
    (
        [DateInterval(1, 10)],
        [DateInterval(1, 10)]
    ),
    (
        [DateInterval(1, 2), DateInterval(2, 10)],
        [DateInterval(1, 10)]
    ),
    (
        [DateInterval(1, 2), DateInterval(3, 10)],
        [DateInterval(1, 2), DateInterval(3, 10)]
    ),
    (
        [DateInterval(1, 2), DateInterval(5, 10)],
        [DateInterval(1, 2), DateInterval(5, 10)]
    ),
    (
        [DateInterval(1, 5), DateInterval(2, 4)],
        [DateInterval(1, 5)]
    ),
    (
        [DateInterval(1, 5), DateInterval(2, 7), DateInterval(10, 12), DateInterval(11, 20)],
        [DateInterval(1, 7), DateInterval(10, 20)]
    ),
    (
        [DateInterval(11, 20), DateInterval(10, 12), DateInterval(2, 7), DateInterval(1, 5)],
        [DateInterval(1, 7), DateInterval(10, 20)]
    ),
))
def test_join_intervals(intervals, expected):
    assert join_intervals(intervals) == expected
