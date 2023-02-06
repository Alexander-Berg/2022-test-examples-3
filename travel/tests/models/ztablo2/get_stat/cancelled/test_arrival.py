# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestArrival(TestCancelledBase):
    """
    Прибывающий на конечную станцию рейс.
    Реального времени прибытия нет.
    """

    def get_cases(self):
        return [
            {
                'arrival': self.just_before_range,
                'arrival_cancelled': True,
                'expected': 0,
                'message': 'Arrival - just before range'
            },
            {
                'arrival': self.range_start,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Arrival - range start'
            },
            {
                'arrival': self.just_after_range_start,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Arrival - just after range start'
            },
            {
                'arrival': self.just_before_range_finish,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Arrival - just before range finish'
            },
            {
                'arrival': self.range_finish,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Arrival - range finish'
            },
            {
                'arrival': self.just_after_range,
                'arrival_cancelled': True,
                'expected': 0,
                'message': 'Arrival - just after range finish'
            }
        ]

    def test_cancelled(self):
        self.process_cases()
