# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_airport, create_tablo_source, create_airport_tablo_source

from travel.avia.backend.main.rest.airport_tablo_source.index import AirportTabloSourceIndexView, AirportTabloSourceIndexForm
from travel.avia.backend.repository.airport_tablo_source import airport_tablo_source_repository


class AirportTabloSourceIndexViewTest(TestCase):
    def setUp(self):
        self._view = AirportTabloSourceIndexView(
            form=AirportTabloSourceIndexForm(),
            repository=airport_tablo_source_repository,
            logger=cast(Logger, Mock()),
        )

        self._airports = [
            create_airport('SVO'),
            create_airport('DME'),
            create_airport('SVX'),
        ]
        self._tablo_source = create_tablo_source(code='test')

    def test_view(self):
        not_empty_list = {
            a.id for a in self._airports[:2]
        }
        for airport in self._airports[:2]:
            create_airport_tablo_source(
                station=airport,
                source=self._tablo_source,
            )

        result = self._view._unsafe_process({})
        result = ujson.loads(result.response[0])

        assert result[u'status'] == u'ok'

        data = result[u'data']

        assert len(data) == len(not_empty_list)
        for d in data:
            assert d['airport_id'] in not_empty_list

    def test_view_trusted(self):
        create_airport_tablo_source(
            station=self._airports[0],
            source=self._tablo_source,
            trusted=True,
        )
        create_airport_tablo_source(
            station=self._airports[1],
            source=self._tablo_source,
            trusted=False,
        )
        create_airport_tablo_source(
            station=self._airports[2],
            source=self._tablo_source,
            trusted=True,
        )

        result = self._view._unsafe_process({'trusted': 1})
        result = ujson.loads(result.response[0])

        assert result[u'status'] == u'ok'

        data = result[u'data']
        assert len(data) == 2
        for d in data:
            assert d['airport_id'] in (self._airports[0].id, self._airports[2].id)

        result = self._view._unsafe_process({'trusted': 0})
        result = ujson.loads(result.response[0])

        assert result[u'status'] == u'ok'

        data = result[u'data']
        assert len(data) == 1
        for d in data:
            assert d['airport_id'] in (self._airports[1].id, )
