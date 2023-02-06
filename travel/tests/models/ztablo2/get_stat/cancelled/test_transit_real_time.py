# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestTransitRealTime(TestCancelledBase):
    """
    Рейс прибывает и отправляется по промежуточной станции.
    Есть реальное временя прибытия и отправления.
    Время прибытия и отправления по графику находится вне диапазона.
    """

    def get_cases(self):
        cases = [
            {
                'real_arrival': self.before_range,
                'real_departure': self.just_before_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Real Arrival - before range, Real Departure - before range'
            },
            {
                'real_arrival': self.before_range,
                'real_departure': self.range_start,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - before range, Real Departure - range start'
            },
            {
                'real_arrival': self.before_range,
                'real_departure': self.just_after_range_start,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - before range, Real Departure - in range'
            },
            {
                'real_arrival': self.range_start,
                'real_departure': self.in_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - range start, Real Departure - in range'
            },
            {
                'real_arrival': self.just_after_range_start,
                'real_departure': self.in_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - in range, Real Departure - in range'
            },
            {
                'real_arrival': self.in_range,
                'real_departure': self.range_finish,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - in range, Real Departure - range finish'
            },
            {
                'real_arrival': self.just_before_range_finish,
                'real_departure': self.after_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - in range, Real Departure - after range finish'
            },
            {
                'real_arrival': self.range_finish,
                'real_departure': self.after_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Real Arrival - range finish, Real Departure - after range finish'
            },
            {
                'real_arrival': self.just_after_range,
                'real_departure': self.after_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Real Arrival - after range finish, Real Departure - after range finish'
            }
        ]

        for case in cases:
            case['arrival'] = self.before_range
            case['departure'] = self.just_before_range

        return cases

    def test_cancelled(self):
        self.process_cases()
