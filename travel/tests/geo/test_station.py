# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from django.db import connection
from django.test.utils import CaptureQueriesContext

from travel.rasp.library.python.common23.models.core.geo.station import Station
from travel.rasp.library.python.common23.models.core.directions.direction import Direction
from travel.rasp.library.python.common23.models.core.directions.direction_marker import DirectionMarker
from travel.rasp.library.python.common23.models.core.directions.station2direction import Station2Direction
from travel.rasp.library.python.common23.tester.factories import create_station, create_code_system
from travel.rasp.library.python.common23.tester.testcase import TestCase


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

    def test_get_code(self):
        """Проверяем, что по коду системы мы можем получить код станции в этой системе.
        Если коды прекешированы, то получаем код из прекэша."""
        create_code_system(code='sys_1')
        create_code_system(code='sys_2')
        codes_1 = {'sys_1': 'st_1_sys_1', 'sys_2': 'st_1_sys_2'}
        codes_2 = {'sys_1': 'st_2_sys_1'}

        station_1 = create_station(__={'codes': codes_1})
        station_2 = create_station(__={'codes': codes_2})
        codes_precache = {station_1.pk: {'sys_2': 'st_1_sys_2_precached'}}
        with mock.patch('travel.rasp.library.python.common23.models.core.geo.station_code_manager.StationCodeManager.code_cache', codes_precache):
            assert station_1.get_code('sys_1') == 'st_1_sys_1'
            assert station_1.get_code('sys_2') == 'st_1_sys_2_precached'
            assert station_2.get_code('sys_1') == 'st_2_sys_1'

    def test_station_direction_precache(self):
        station_with_marker = create_station()
        station_without_marker = create_station()
        direction = Direction.objects.create(title='Dir 1')
        DirectionMarker.objects.create(station=station_with_marker, direction=direction, order=1)

        with Direction.objects.using_precache():
            with CaptureQueriesContext(connection) as queries:
                assert station_with_marker.get_direction() == direction
                assert station_without_marker.get_direction() is None
            assert len(queries) == 2

        with Station2Direction.using_precache(), Direction.objects.using_precache():
            assert Station2Direction.is_precached()
            with CaptureQueriesContext(connection) as queries:
                assert station_with_marker.get_direction() == direction
                assert station_without_marker.get_direction() is None
            assert len(queries) == 0

        assert not Station2Direction.is_precached()
