# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.late.base import TestLateBase


class TestNotLate(TestLateBase):
    """
    Рейс не задерживается
    """

    def get_cases(self):
        return [
            {
                'arrival': self.now,
                'departure': self.departure_future_timestamp,
                'expected': 0,
                'message': 'Arrival - now, Departure - future, Real Arrival - None, Real Departure - None'
            },
            {
                'arrival': self.now - self.arrival_delta - self.minute,
                'departure': self.now - self.departure_delta - self.minute,
                'expected': 0,
                'message': 'Arrival - now-21, Departure - now-21, Real Arrival - None, Real Departure - None'
            },
            {
                'arrival': self.now,
                'real_arrival': self.now,
                'departure': self.departure_future_timestamp,
                'real_departure': self.departure_future_timestamp,
                'expected': 0,
                'message': 'Arrival == Real Arrival, Departure == Real Departure'
            }
        ]

    def test_late(self):
        self.process_cases()
