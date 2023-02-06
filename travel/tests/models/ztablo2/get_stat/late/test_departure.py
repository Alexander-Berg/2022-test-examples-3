# -*- coding: utf-8 -*-

from stationschedule.tests.models.ztablo2.get_stat.late.base import TestLateBase


class TestDeparture(TestLateBase):
    """
    Тестирование задержек по отправлению
    """

    def get_cases(self):
        return [
            {
                'departure': self.now - self.minute - self.departure_delta,
                'real_departure': self.now - self.minute,
                'expected': 0,
                'message': 'Departure - now-21, Real Departure - now-1'
            },
            {
                'departure': self.now - self.departure_delta,
                'real_departure': self.now,
                'expected': 1,
                'message': 'Departure - now-20, Real Departure - now'
            },
            {
                'departure': self.now - self.departure_delta + self.minute,
                'real_departure': self.now,
                'expected': 0,
                'message': 'Departure - now-19, Real Departure - now'
            },
            {
                'departure': self.now - self.minute,
                'real_departure': self.now - self.minute + self.departure_delta,
                'expected': 1,
                'message': 'Departure - now-1, Real Departure - now+19'
            },
            {
                'departure': self.now - self.minute,
                'real_departure': self.now + self.departure_delta - self.minute - self.minute,
                'expected': 0,
                'message': 'Departure - now-1, Real Departure - now+18'
            },
            {
                'departure': self.now,
                'real_departure': self.now + self.departure_delta,
                'expected': 1,
                'message': 'Departure - now, Real Departure - now+20'
            },
            {
                'departure': self.now,
                'real_departure': self.now + self.departure_delta - self.minute,
                'expected': 0,
                'message': 'Departure - now, Real Departure - now+19'
            },
            {
                'departure': self.departure_future_timestamp,
                'real_departure': self.departure_future_timestamp + self.departure_delta,
                'expected': 1,
                'message': 'Departure - now+30, Real Departure - now+50'
            },
            {
                'departure': self.departure_future_timestamp + self.minute,
                'real_departure': self.departure_future_timestamp + self.minute + self.departure_delta,
                'expected': 0,
                'message': 'Departure - now+31, Real Departure - now+51'
            },
        ]

    def test_late(self):
        self.process_cases()
