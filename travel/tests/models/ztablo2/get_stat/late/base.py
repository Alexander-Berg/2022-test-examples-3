# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import mock

from common.tester.factories import create_station
from common.tester.testcase import TestCase
from stationschedule.models import ZTablo2


class TestLateBase(TestCase):
    """
    Базовый класс для тестирования опаздывающих рейсов.
    RASPFRONT-1334
    Задержанными считать только те рейсы, у которых:
    - для вылета - время вылета уже в прошлом, а факт ещё в будущем (при разнице между ними более 20 минут),
        или время вылета не позднее +30 минут, а факт отличается от него более чем на 20 минут.
    - для прилета - время прилета уже в прошлом более чем на 20 минут, а факта нет
        или время прилета в будущем не более чем на +120 минут, а факт отличается от него более чем на +20 минут.
    """

    def setUp(self):
        self.minute = timedelta(minutes=1)
        self.now = datetime(2015, 9, 16, 20, 0)

        self.departure_delta = timedelta(minutes=30)
        self.departure_future_timestamp = datetime(2015, 9, 16, 20, 30)

        self.arrival_delta = timedelta(minutes=30)
        self.arrival_future_timestamp = datetime(2015, 9, 16, 21, 0)

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
        actual = stat.get(station_id, {}).get('late')
        expected = case['expected']

        assert actual == expected, '{}. Actual - {}, expected - {}. station_id - {}'.format(case['message'], actual, expected, station_id)
