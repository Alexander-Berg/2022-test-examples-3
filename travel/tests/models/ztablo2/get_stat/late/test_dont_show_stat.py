# -*- coding: utf-8 -*-

from stationschedule.tests.models.ztablo2.get_stat.late.base import TestLateBase


class TestDontShowStat(TestLateBase):
    """
    У станции  снят чекбокс "Показывать задержки и отмены"
    """

    def show_tablo_stat(self):
        return False

    def get_cases(self):
        return [
            {
                'arrival': self.now,
                'real_arrival': self.now + self.arrival_delta + self.minute,
                'departure': self.now,
                'real_departure': self.now + self.arrival_delta + self.minute,
                'message': 'Arrival - now, Departure - now, Real Arrival - now+21, Real Deprture - now+21'
            }
        ]

    def test_late(self):
        self.process_cases()
