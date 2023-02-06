# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestNotCancelled(TestCancelledBase):
    """
    У рейса нет признака отмены
    """

    def get_cases(self):
        return [
            {
                'arrival': self.just_after_range_start,
                'real_arrival': self.just_after_range_start,
                'departure': self.in_range,
                'real_departure': self.in_range,
                'expected': 0,
                'message': 'Not cancelled'
            }
        ]

    def test_cancelled(self):
        self.process_cases()
