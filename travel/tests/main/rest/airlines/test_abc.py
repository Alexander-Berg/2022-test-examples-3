# coding=utf-8
from __future__ import absolute_import

import ujson
from functools import partial
from logging import Logger

from marshmallow import ValidationError
from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.airlines.abc import AirlinesForm, AirlinesByAbcView
from travel.avia.backend.main.rest.airlines.helpers import AbcAirlineProvider
from travel.avia.backend.tests.factories import create_airline_model
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class AirlinesFormTest(TestCase):
    def setUp(self):
        self._form = AirlinesForm()

    def build_query(self, start, end):
        return {
            u'national_version': u'ru',
            u'lang': u'ru',
            u'start': start,
            u'end': end,
        }

    def assert_raises(self, start, end, message):
        try:
            self._form.load(self.build_query(start, end))
        except ValidationError as e:
            assert e.message == message

    def test_from_another_abc(self):
        self.assert_raises(u'!', u'?', {
            u'start': u'[!] or/and [?] are not in abcs.',
            u'end': u'[!] or/and [?] are not in abcs.',
        })

    def test_mix_order(self):
        self.assert_raises(u'я', u'а', {
            u'start': u'Start [я] must be less than end [а]. Abc: [абвгдеёжзийклмнопрстуфхцчшщъыьэюя]',
            u'end': u'Start [я] must be less than end [а]. Abc: [абвгдеёжзийклмнопрстуфхцчшщъыьэюя]',
        })

    def test_normal(self):
        data, errors = self._form.load(self.build_query(u'а', u'г'))
        assert not errors

        assert data == {
            u'lang': u'ru',
            u'national_version': u'ru',
            u'start': u'а',
            u'end': u'г',
            u'letters': u'абвг',
        }


class AirlinesByAbcViewTest(TestCase):
    def setUp(self):
        self._fake_abc_airline_provider = Mock()
        self._translated_title_repository = TranslatedTitleRepository()
        self._view = AirlinesByAbcView(
            abc_airline_provider=cast(AbcAirlineProvider, self._fake_abc_airline_provider),
            logger=cast(Logger, Mock())
        )

    def test_view(self):
        create_airline = partial(
            create_airline_model,
            translated_title_repository=self._translated_title_repository
        )
        airlines = [
            create_airline(u'а', slug='slug1', iata=None, sirena=None),
            create_airline(u'аа', slug='slug2', iata=u'ZZ', sirena=None),
            create_airline(u'в', slug='slug3', iata=None, sirena=u'АУ'),
            create_airline(u'вв', slug='slug4', iata=u'TT', sirena=u'ВВ')
        ]

        self._fake_abc_airline_provider.get_airlines_by_letter = Mock(
            return_value={
                u'а': [airlines[0], airlines[1]],
                u'б': [],
                u'в': [airlines[2], airlines[3]]
            }
        )
        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'start': u'а',
            'end': u'в',
        })

        assert ujson.loads(result.response[0]) == {
            u'status': u'ok',
            u'data': [
                {
                    u'airlines': [
                        {
                            u'slug': u'slug1',
                            u'id': 1,
                            u'title': u'а'
                        },
                        {
                            u'slug': u'slug2',
                            u'id': 1,
                            u'title': u'аа (ZZ)'
                        }
                    ],
                    u'letter': u'а'
                },
                {
                    u'airlines': [],
                    u'letter': u'б'
                },
                {
                    u'airlines': [
                        {
                            u'slug': u'slug3',
                            u'id': 1,
                            u'title': u'в (АУ)'
                        },
                        {
                            u'slug': u'slug4',
                            u'id': 1,
                            u'title': u'вв (TT)'
                        }
                    ],
                    u'letter': u'в'
                }
            ]
        }
