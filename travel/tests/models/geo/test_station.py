# -*- coding=utf-8 -*-

from travel.avia.library.python.common.models.geo import Station
from travel.avia.library.python.tester.factories import create_station, create_code_system
from travel.avia.library.python.tester.testcase import TestCase


class TestStation(TestCase):
    def test_get_by_code(self):
        """Проверяем, что при использовании системы кодирования 'yandex' получаем станцию по id.
        Проверяем получение станции с помощью других систем кодирования."""
        create_code_system(code='sys_1')
        create_code_system(code='sys_2')
        codes_1 = {'sys_1': 'st_1_sys_1', 'sys_2': 'st_1_sys_2'}
        codes_2 = {'sys_1': 'st_2_sys_1'}

        stations = [
            create_station(__={'codes': codes_1}),
            create_station(__={'codes': codes_2})
        ]

        for code_sys, code in codes_1.items():
            assert stations[0] == Station.get_by_code(code_sys, code)

        for code_sys, code in codes_2.items():
            assert stations[1] == Station.get_by_code(code_sys, code)

        for station in stations:
            assert station == Station.get_by_code('yandex', station.id)
