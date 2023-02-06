# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock

from common.tester.factories import create_station
from common.tester.testcase import TestCase
from stationschedule.models import ZTablo2


class TestCancelledBase(TestCase):
    """
    Базовый класс для тестирования отмененных рейсов.

    RASPFRONT-1334
    Отменённые считаем по-прежнему - те рейсы, которые явно помечены у поставщика как отменённые
    и у которых факт или расписание попадают в диапазон [-2ч, +24ч]
    """

    def setUp(self):
        self.now = datetime(2015, 9, 16, 20, 0)
        self.range_start = datetime(2015, 9, 16, 18, 0)
        self.range_finish = datetime(2015, 9, 17, 20, 0)

        self.before_range = datetime(2015, 9, 16, 17, 0)
        self.just_before_range = datetime(2015, 9, 16, 17, 59)

        self.just_after_range_start = datetime(2015, 9, 16, 18, 1)
        self.in_range = datetime(2015, 9, 17, 0, 0)
        self.just_before_range_finish = datetime(2015, 9, 17, 19, 59)

        self.just_after_range = datetime(2015, 9, 17, 20, 1)
        self.after_range = datetime(2015, 9, 17, 21, 0)

    def get_cases(self):
        return []

    def show_tablo_stat(self):
        return True

    def process_cases(self):
        stations = []
        cases = self.get_cases()

        for index, case in enumerate(cases):
            station_id = index + 1
            station = create_station(id=station_id, tablo_state='real', show_tablo_stat=self.show_tablo_stat())
            stations.append(station)
            case['station_id'] = station_id

            ZTablo2.objects.create(
                station=station,
                arrival=case.get('arrival'), departure=case.get('departure'),
                real_arrival=case.get('real_arrival'), real_departure=case.get('real_departure'),
                arrival_cancelled=case.get('arrival_cancelled', False), departure_cancelled=case.get('departure_cancelled', False))

        with mock.patch('travel.rasp.library.python.common23.date.environment.now', return_value=self.now) as m_now:
            stat = ZTablo2.get_stat_by_stations(stations)

            if self.show_tablo_stat():
                assert len(stat.keys()) == len(stations)

                for case in cases:
                    self.assert_case(stat, case)

            else:
                assert stat == {}

        if self.show_tablo_stat():
            m_now.assert_called_once_with()
        else:
            m_now.assert_not_called()

    def assert_case(self, stat, case):
        station_id = case['station_id']
        actual = stat.get(station_id, {}).get('cancelled')
        expected = case['expected']

        assert actual == expected, '{}. Actual - {}, expected - {}. station_id - {}'.format(case['message'], actual, expected, station_id)
