# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestTransit(TestCancelledBase):
    """
    Рейс прибывает и отправляется по промежуточной станции.
    Реального времени прибытия и отправления нет.
    """

    def get_cases(self):
        return [
            {
                'arrival': self.before_range,
                'departure': self.just_before_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Arrival - before range, Departure - before range'
            },
            {
                'arrival': self.before_range,
                'departure': self.range_start,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Arrival - before range, Departure - range start'
            },
            {
                'arrival': self.before_range,
                'departure': self.just_after_range_start,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Arrival - before range, Departure - in range'
            },
            {
                'arrival': self.range_start,
                'departure': self.in_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Arrival - range start, Departure - in range'
            },
            {
                'arrival': self.just_after_range_start,
                'departure': self.in_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Arrival - in range, Departure - in range'
            },
            {
                'arrival': self.in_range,
                'departure': self.range_finish,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Arrival - in range, Departure - range finish'
            },
            {
                'arrival': self.just_before_range_finish,
                'departure': self.after_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Arrival - in range, Departure - after range finish'
            },
            {
                'arrival': self.range_finish,
                'departure': self.after_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 1,
                'message': 'Arrival - range finish, Departure - after range finish'
            },
            {
                'arrival': self.just_after_range,
                'departure': self.after_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'expected': 0,
                'message': 'Arrival - after range finish, Departure - after range finish'
            }
        ]

    def test_cancelled(self):
        self.process_cases()
