# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestDeparture(TestCancelledBase):
    """
    Отправляющийся с начальной станции рейс.
    Реального времени отправления нет.
    """

    def get_cases(self):
        return [
            {
                'departure': self.just_before_range,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Departure - just before range'
            },
            {
                'departure': self.range_start,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Departure - range start'
            },
            {
                'departure': self.just_after_range_start,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Departure - just after range start'
            },
            {
                'departure': self.just_before_range_finish,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Departure - just before range finish'
            },
            {
                'departure': self.range_finish,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Departure - range finish'
            },
            {
                'departure': self.just_after_range,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Departure - just after range finish'
            }
        ]

    def test_cancelled(self):
        self.process_cases()
