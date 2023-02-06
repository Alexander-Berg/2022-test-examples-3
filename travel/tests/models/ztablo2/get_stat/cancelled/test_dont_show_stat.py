# -*- coding: utf-8 -*-
from stationschedule.tests.models.ztablo2.get_stat.cancelled.base import TestCancelledBase


class TestDontShowStat(TestCancelledBase):
    """
    Рейс прибывает и отправляется по промежуточной станции.
    Реального времени прибытия и отправления нет.
    """

    def show_tablo_stat(self):
        return False

    def get_cases(self):
        return [
            {
                'arrival': self.in_range,
                'departure': self.in_range,
                'arrival_cancelled': True,
                'departure_cancelled': True,
                'message': 'Arrival - in range, Departure - in range'
            }
        ]

    def test_cancelled(self):
        self.process_cases()
