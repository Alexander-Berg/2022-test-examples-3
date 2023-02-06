# -*- coding: utf-8 -*-
from __future__ import absolute_import

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_airport, create_tablo_source, create_airport_tablo_source

from travel.avia.backend.repository.airport_tablo_source import AirportTabloSourceRepository


class TestAirportTabloSourceRepository(TestCase):
    def setUp(self):
        self._repository = AirportTabloSourceRepository()

        self._airports = [
            create_airport('SVO'),
            create_airport('DME'),
            create_airport('SVX'),
        ]
        self._tablo_source = create_tablo_source(code='test')

    def test_get_not_empty(self):
        not_empty_list = {
            a.id for a in self._airports[:2]
        }
        for airport in self._airports[:2]:
            create_airport_tablo_source(
                station=airport,
                source=self._tablo_source,
            )

        result = self._repository.get_not_empty()

        assert len(result) == len(not_empty_list)
        for a in result:
            assert a.airport_id in not_empty_list
