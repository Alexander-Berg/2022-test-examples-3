# -*- coding: utf-8 -*-

import pytest

from travel.rasp.admin.scripts.schedule.bus.dyc import check_and_filter_times_for_every_other_day_mask


@pytest.mark.parametrize('times', [
    ['10:00', '10:01'],
    ['10:00', '10:01', '20:45', '20:46'],
    ['10:01', '10:00'],
    ['10:01', '10:00', '20:45', '20:44'],
    ['23:59', '00:00'],
    ['00:00', '23:59'],
])
def test_good_times_for_every_other_day_mask(times):
    new_times = check_and_filter_times_for_every_other_day_mask(times)

    assert len(new_times) * 2 == len(times)
    assert all(t in times for t in new_times)


@pytest.mark.parametrize('times', [
    ['10:00', '10:10'],
    ['10:00', '10:01', '20:45', '20:56'],
    ['10:00', '10:01', '20:45', '60:56'],
    ['10:01', ],
    ['10:01', '10:00', '20:45', ],
    [],
    ['10:01', '60:00'],
    ['60:01', '10:00'],
    ['00:15', '23:59'],
    ['23:59', '00:15'],
])
def test_bad_times_for_every_other_day_mask(times):
    new_times = check_and_filter_times_for_every_other_day_mask(times)

    assert not new_times
