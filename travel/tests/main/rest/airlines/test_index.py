# coding=utf-8
from __future__ import absolute_import

import ujson

from collections import defaultdict
from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.backend.main.rest.airlines.helpers import AbcAirlineProvider
from travel.avia.backend.main.rest.airlines.index import AirlineIndexView
from travel.avia.library.python.tester.testcase import TestCase


class AirlineIndexViewTest(TestCase):
    def setUp(self):
        self._fake_abc_airline_provider = Mock()
        self._fake_abc_airline_provider.RUSSIAN_ABC_LETTERS = AbcAirlineProvider.RUSSIAN_ABC_LETTERS
        self._fake_abc_airline_provider.ABC_LETTERS = AbcAirlineProvider.ABC_LETTERS

        self._view = AirlineIndexView(
            abc_airline_provider=cast(AbcAirlineProvider, self._fake_abc_airline_provider),
            logger=cast(Logger, Mock())
        )

    def test_view(self):
        airlines_by_letter = defaultdict(list)
        airlines_by_letter[u'я'] = [1, 2, 3]
        airlines_by_letter[u'в'] = [1]
        airlines_by_letter[u'u'] = [1, 2, 3, 4]
        airlines_by_letter[u'p'] = [1, 8]

        self._fake_abc_airline_provider.get_airlines_by_letter = Mock(
            return_value=airlines_by_letter
        )

        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
        })

        result = ujson.loads(result.response[0])
        assert result[u'status'] == u'ok'
        data = result[u'data']
        assert len(data[u'russian_abc_index']) == 33
        assert len(data[u'abc_index']) == 26

        not_empty = {u'я', u'в', u'u', u'p'}

        for item in data[u'russian_abc_index'] + data[u'abc_index']:
            if item[u'letter'] not in not_empty:
                assert item[u'count'] == 0

        def assert_letter(index, letter, count):
            item = None
            for i in index:
                if i[u'letter'] == letter:
                    item = i
                    break
            assert item
            assert item[u'count'] == count

        assert_letter(result[u'data'][u'russian_abc_index'], u'я', 3)
        assert_letter(result[u'data'][u'russian_abc_index'], u'в', 1)
        assert_letter(result[u'data'][u'abc_index'], u'u', 4)
        assert_letter(result[u'data'][u'abc_index'], u'p', 2)
