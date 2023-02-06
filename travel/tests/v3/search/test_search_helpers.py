# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

from hamcrest import assert_that, contains, has_entries

from travel.rasp.api_public.api_public.v3.search.helpers import make_segment_mask_schedule, make_thread_method_link


def test_make_segment_mask_schedule():
    run_days = {
        "2020": {
            "12": [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            "11": [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        },
        "2021": {
            "1": [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }
    }

    schedule = make_segment_mask_schedule(run_days)

    assert_that(schedule, contains(
        has_entries({
            "year": "2021", "month": "1",
            "days": [0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }),
        has_entries({
            "year": "2020", "month": "11",
            "days": [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }),
        has_entries({
            "year": "2020", "month": "12",
            "days": [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }),
    ))


def test_make_thread_method_link():
    thread_link = make_thread_method_link("url/?", date(2020, 12, 12), "uid")

    assert thread_link == "url/?date=2020-12-12&uid=uid"
