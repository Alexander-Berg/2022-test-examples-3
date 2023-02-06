# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestDepartureRealTime(TestCancelledBase):
    """
    Отправляющийся с начальной станции рейс.
    Есть реальное временя отправления.
    Время отправления по графику находится вне диапазона.
    """

    def get_cases(self):
        cases = [
            {
                'real_departure': self.just_before_range,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Real Departure - just before range'
            },
            {
                'real_departure': self.range_start,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Departure - range start'
            },
            {
                'real_departure': self.just_after_range_start,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Departure - just after range start'
            },
            {
                'real_departure': self.just_before_range_finish,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Departure - just before range finish'
            },
            {
                'real_departure': self.range_finish,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Departure - range finish'
            },
            {
                'real_departure': self.just_after_range,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Real Departure - just after range finish'
            }
        ]

        # По графику - отправление - вне диапазона
        for case in cases:
            case['departure'] = self.before_range

        return cases

    def test_cancelled(self):
        self.process_cases()
