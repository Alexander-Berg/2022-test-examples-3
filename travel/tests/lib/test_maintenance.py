# -*- coding: utf-8 -*-

from datetime import time, datetime, timedelta

from travel.rasp.admin.lib.maintenance.flags import MaintenanceState
from travel.rasp.admin.lib.unittests.testcase import TestCase


class TestMaintenanceState(TestCase):
    def testWarn(self):
        state = MaintenanceState('1', 1, '1', block_admins=['blue'],
                                 planned_launch_time=time(1, 0), days=[])
        now = datetime(2013, 9, 30, 0,)
        self.assertEqual(state.get_remaining_seconds(now), 60 * 60)
        self.assertFalse(state.need_to_warn(now))

        now += timedelta(minutes=10)

        self.assertEqual(state.get_remaining_seconds(now), 60 * 50)
        self.assertTrue(state.need_to_warn(now))

    def testDays(self):
        state = MaintenanceState('1', 1, '1', block_admins=['blue'],
                                 planned_launch_time=time(1, 0), days=[1])
        now = datetime(2013, 9, 30, 0, 10)  # поднедельник
        self.assertEqual(state.get_remaining_seconds(now), 60 * 50)
        self.assertTrue(state.need_to_warn(now))

        now += timedelta(1)

        self.assertEqual(state.get_remaining_seconds(now), None)
        self.assertFalse(state.need_to_warn(now))
