# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.testcase import TestCase
from common.tester.factories import create_station, create_thread
from travel.rasp.library.python.common23.date import environment
from stationschedule.type.base import BaseIntervalSchedule


class TestIntervalSchedule(TestCase):
    def test_not_schow_arrival_at_last_station_routes(self):
        station_from = create_station(t_type='bus')
        station_to = create_station(t_type='bus')
        thread = create_thread(  # noqa
            type='interval', t_type='bus',
            begin_time='10:00', end_time='22:00',
            schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ]
        )

        interval_schedule = BaseIntervalSchedule(station_from, all_days_next_days=60)
        interval_schedule.build(schedule_date=environment.today())
        assert len(interval_schedule.schedule_routes) == 1

        interval_schedule = BaseIntervalSchedule(station_to, all_days_next_days=60)
        interval_schedule.build(schedule_date=environment.today())
        assert len(interval_schedule.schedule_routes) == 0
