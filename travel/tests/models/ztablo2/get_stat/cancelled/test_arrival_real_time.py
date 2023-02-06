# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestArrivalRealTime(TestCancelledBase):
    """
    Прибывающий на конечную станцию рейс.
    Есть реальное временя прибытия.
    Время прибытия по графику находится вне диапазона.
    """

    def get_cases(self):
        cases = [
            {
                'real_arrival': self.just_before_range,
                'arrival_cancelled': True,
                'expected': 0,
                'message': 'Real Arrival - just before range'
            },
            {
                'real_arrival': self.range_start,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - range start'
            },
            {
                'real_arrival': self.just_after_range_start,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - just after range start'
            },
            {
                'real_arrival': self.just_before_range_finish,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - just before range finish'
            },
            {
                'real_arrival': self.range_finish,
                'arrival_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - range finish'
            },
            {
                'real_arrival': self.just_after_range,
                'arrival_cancelled': True,
                'expected': 0,
                'message': 'Real Arrival - just after range finish'
            }
        ]

        # По графику - прибытие - вне диапазона
        for case in cases:
            case['arrival'] = self.before_range

        return cases

    def test_cancelled(self):
        self.process_cases()
