# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ
from travel.rasp.wizards.train_wizard_api.direction.segments import make_tomorrow_query
from travel.rasp.wizards.wizard_lib.tests_utils import msk_dt
from travel.rasp.wizards.wizard_lib.event_date_query import EventDateQuery, limit_segments_with_event_date


@replace_now('2000-01-01')
def test_make_tomorrow_query():
    assert make_tomorrow_query(MSK_TZ) == EventDateQuery(msk_dt(2000, 1, 2), limit_segments_with_event_date)
