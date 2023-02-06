# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import mock

from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ
from travel.rasp.wizards.wizard_lib.event_date_query import EventDateQuery, limit_segments_with_event_date, make_event_date_query
from travel.rasp.wizards.wizard_lib.tests_utils import msk_dt


@replace_now('2000-01-01')
def test_make_event_date_query():
    assert make_event_date_query(None, MSK_TZ) == EventDateQuery(msk_dt(2000, 1, 1), limit_segments_with_event_date)

    date_query = make_event_date_query(date(2000, 6, 1), MSK_TZ)
    assert date_query == EventDateQuery(msk_dt(2000, 6, 1), mock.ANY)
    assert [
        raw_segment.event_dt
        for raw_segment in date_query.raw_filter_func((
            mock.Mock(event_dt=msk_dt(2000, 6, 1, 12)),
            mock.Mock(event_dt=msk_dt(2000, 6, 1, 13)),
            mock.Mock(event_dt=msk_dt(2000, 6, 2)),
        ))
    ] == [
        msk_dt(2000, 6, 1, 12),
        msk_dt(2000, 6, 1, 13),
    ]
