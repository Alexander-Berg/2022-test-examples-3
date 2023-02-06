# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from common.utils.date import UTC_TZ
from travel.rasp.wizards.suburban_wizard_api.lib.direction.segments import filter_next_segments
from travel.rasp.wizards.wizard_lib.tests_utils import utc_dt


@pytest.mark.parametrize('segments_dt, expected_dt', (
    # border is kept on 2000-01-02T00:00 because 3 segments is enough
    (
        (
            utc_dt(2000, 1, 1, 0),
            utc_dt(2000, 1, 1, 1),
            utc_dt(2000, 1, 1, 2),
            utc_dt(2000, 1, 2, 0),
            utc_dt(2000, 1, 2, 1),
        ),
        (
            utc_dt(2000, 1, 1, 0),
            utc_dt(2000, 1, 1, 1),
            utc_dt(2000, 1, 1, 2),
        )
    ),
    # border is switched to 2000-01-02T04:00 because 1 segment is not enough
    (
        (
            utc_dt(2000, 1, 1, 0),
            utc_dt(2000, 1, 2, 0),
            utc_dt(2000, 1, 2, 3),
            utc_dt(2000, 1, 2, 4),
        ),
        (
            utc_dt(2000, 1, 1, 0),
            utc_dt(2000, 1, 2, 0),
            utc_dt(2000, 1, 2, 3),
        )
    ),
    # border is kept on 2000-01-02T04:00 because 3 segments is enough
    (
        (
            utc_dt(2000, 1, 1, 7),
            utc_dt(2000, 1, 1, 8),
            utc_dt(2000, 1, 1, 9),
            utc_dt(2000, 1, 2, 4),
            utc_dt(2000, 1, 2, 5),
        ),
        (
            utc_dt(2000, 1, 1, 7),
            utc_dt(2000, 1, 1, 8),
            utc_dt(2000, 1, 1, 9),
        )
    ),
    # border is switched to 2000-01-02T07:00 because 1 segment is not enough
    (
        (
            utc_dt(2000, 1, 1, 7),
            utc_dt(2000, 1, 2, 5),
            utc_dt(2000, 1, 2, 6),
            utc_dt(2000, 1, 2, 7),
        ),
        (
            utc_dt(2000, 1, 1, 7),
            utc_dt(2000, 1, 2, 5),
            utc_dt(2000, 1, 2, 6),
        )
    ),
))
def test_filter_next_segments(segments_dt, expected_dt):
    segments_iter = (mock.Mock(departure_dt=segment_dt) for segment_dt in segments_dt)
    assert tuple(
        segment.departure_dt for segment in filter_next_segments(segments_iter, local_tz=UTC_TZ)
    ) == expected_dt
