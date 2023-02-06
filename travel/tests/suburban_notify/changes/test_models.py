# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from travel.rasp.info_center.info_center.suburban_notify.changes.models import (
    SubscriptionChanges, Change, ThreadData, RTSChange
)


class TestSubscriptionChange(object):
    def test_to_dict(self):
        sub_changes = SubscriptionChanges(
            calc_date=datetime(2019, 10, 11),
            uid=42,
            point_from_key='s2',
            point_to_key='c23',
            interval_from=100,
            interval_to=1440,
            changes=[
                Change(
                    type=1,
                    start_date=2,
                    basic_thread=ThreadData(key=31, is_first_run_day=32, uid=33, number=34, title='35'),
                    rel_thread=ThreadData(key=41, is_first_run_day=42, uid=43, number=44, title='45'),
                    rts_from=RTSChange(
                        type=51, station=52, actual_time=53, schedule_time=54,
                        diff=55, first_station=56, last_station=57,
                    ),
                    rts_to=RTSChange(
                        type=61, station=62, actual_time=63, schedule_time=64,
                        diff=65, first_station=66, last_station=67,
                    ),
                    push_sent=True,
                ),
                Change(
                    type=2,
                    start_date=3,
                    basic_thread=None,
                    rel_thread=None,
                    rts_from=None,
                    rts_to=None,
                    push_sent=False,
                ),
            ]
        )

        assert sub_changes.to_dict() == dict(
            calc_date=datetime(2019, 10, 11),
            uid=42,
            point_from_key='s2',
            point_to_key='c23',
            interval_from=100,
            interval_to=1440,
            changes=[
                dict(
                    type=1,
                    start_date=2,
                    basic_thread=dict(key=31, is_first_run_day=32, uid=33, number=34, title='35'),
                    rel_thread=dict(key=41, is_first_run_day=42, uid=43, number=44, title='45'),
                    rts_from=dict(
                        type=51, station=52, actual_time=53, schedule_time=54,
                        diff=55, first_station=56, last_station=57,
                    ),
                    rts_to=dict(
                        type=61, station=62, actual_time=63, schedule_time=64,
                        diff=65, first_station=66, last_station=67,
                    ),
                    push_sent=True,
                    hash=sub_changes.changes[0].__hash__()
                ),
                dict(
                    type=2,
                    start_date=3,
                    basic_thread=None,
                    rel_thread=None,
                    rts_from=None,
                    rts_to=None,
                    push_sent=False,
                    hash=sub_changes.changes[1].__hash__()
                ),
            ]
        )


class TestChange(object):
    def test_comparation(self):
        sc1 = Change(
            type=1,
            start_date=2,
            basic_thread=ThreadData(key=31, is_first_run_day=32),
            rel_thread=ThreadData(key=41, is_first_run_day=42),
            rts_from=RTSChange(
                type=51, station=52, actual_time=53, schedule_time=54,
                diff=55, first_station=56, last_station=57,
            ),
            rts_to=RTSChange(
                type=61, station=62, actual_time=63, schedule_time=64,
                diff=65, first_station=66, last_station=67,
            ),
        )

        sc2 = Change(
            type=1,
            start_date=2,
            basic_thread=ThreadData(key=31, is_first_run_day=32),
            rel_thread=ThreadData(key=41, is_first_run_day=42),
            rts_from=RTSChange(
                type=51, station=52, actual_time=53, schedule_time=54,
                diff=55, first_station=56, last_station=57,
            ),
            rts_to=RTSChange(
                type=61, station=62, actual_time=63, schedule_time=64,
                diff=65, first_station=66, last_station=67,
            ),
        )

        assert sc1 == sc2
        sc1.type = 2
        assert sc1 != sc2
        sc1.type = 1
        assert sc1 == sc2

        sc2.basic_thread = None
        assert sc1 != sc2
        sc1.basic_thread = None
        assert sc1 == sc2

        sc1.rts_to = None
        assert sc1 != sc2
        sc2.rts_to = None
        assert sc1 == sc2


class TestThreadData(object):
    def test_comparation(self):
        td1 = ThreadData(key=31, is_first_run_day=False)
        td2 = ThreadData(key=31, is_first_run_day=False)

        assert td1 == td2
        td1.is_first_run_day = True
        assert td1 != td2


class TestRTSChage(object):
    def test_comparation(self):
        rc1 = RTSChange(
            type=51, station=52, actual_time=53, schedule_time=54,
            diff=55, first_station=56, last_station=57,
        )
        rc2 = RTSChange(
            type=51, station=52, actual_time=53, schedule_time=54,
            diff=55, first_station=56, last_station=57,
        )

        assert rc1 == rc2
        rc2.schedule_time = 8
        assert rc1 != rc2
