# -*- coding: utf-8 -*-

from stationschedule.tests.models.ztablo2.get_stat.late.base import TestLateBase


class TestArrival(TestLateBase):
    """
    Тестирование задержек по прибытию
    """

    def get_cases(self):
        return [
            {
                'arrival': self.now - self.arrival_delta - self.minute,
                'real_arrival': self.now - self.minute,
                'expected': 0,
                'message': 'Arrival - now-21, Real Arrival - now-1'
            },
            {
                'arrival': self.now - self.arrival_delta,
                'real_arrival': self.now,
                'expected': 1,
                'message': 'Arrival - now-20, Real Arrival - now'
            },
            {
                'arrival': self.now - self.arrival_delta + self.minute,
                'real_arrival': self.now,
                'expected': 0,
                'message': 'Arrival - now-19, Real Arrival - now'
            },
            {
                'arrival': self.now,
                'real_arrival': self.now + self.arrival_delta,
                'expected': 1,
                'message': 'Arrival - now, Real Arrival - now+20'
            },
            {
                'arrival': self.now,
                'real_arrival': self.now + self.arrival_delta - self.minute,
                'expected': 0,
                'message': 'Arrival - now, Real Arrival - now+19'
            },
            {
                'arrival': self.arrival_future_timestamp,
                'real_arrival': self.arrival_future_timestamp + self.arrival_delta,
                'expected': 1,
                'message': 'Arrival - now+60, Real Arrival - now+80'
            },
            {
                'arrival': self.arrival_future_timestamp + self.minute,
                'real_arrival': self.arrival_future_timestamp + self.minute + self.arrival_delta,
                'expected': 0,
                'message': 'Arrival - now+61, Real Arrival - now+81'
            }
        ]

    def test_late(self):
        self.process_cases()
