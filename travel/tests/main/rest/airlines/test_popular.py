# coding=utf-8
from __future__ import absolute_import

import ujson
from functools import partial
from logging import Logger

from mock import Mock
from typing import cast
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.airlines.popular import AirlinesByPopularView
from travel.avia.backend.tests.factories import create_airline_model
from travel.avia.backend.repository.airlines import AirlineRepository
from travel.avia.backend.repository.helpers import NationalBox
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class AirlinesByPopularViewTest(TestCase):
    def setUp(self):
        self._translated_title_repository = TranslatedTitleRepository()
        self._fake_airline_repository = Mock()
        self._view = AirlinesByPopularView(
            airline_repository=cast(
                AirlineRepository, self._fake_airline_repository
            ),
            logger=cast(Logger, Mock())
        )

    def test_view(self):
        create_airline = partial(
            create_airline_model,
            translated_title_repository=self._translated_title_repository
        )
        airlines = [
            create_airline(
                u'аэрофлот', slug=u'slug-1', iata=u'ZZ', sirena=None,
                popular_score_by_national_version=NationalBox({'ru': 10, 'ua': 5}),
            ),
            create_airline(
                u'атлантида', slug=u'slug-2', iata=None, sirena=u'АУ',
                popular_score_by_national_version=NationalBox({'ru': 10, 'ua': 1}),

            ),
            create_airline(
                u'авиалинии', slug=u'slug-3', iata=None, sirena=None,
                popular_score_by_national_version=NationalBox({'ru': 1, 'ua': 10}),
            ),
            create_airline(
                u'not popular', slug=u'slug-4', iata=None, sirena=None,
                popular_score_by_national_version=NationalBox({'ua': 15}),
            ),
        ]
        self._fake_airline_repository.get_all = Mock(return_value=airlines)

        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'count': 10,
        })

        result = ujson.loads(result.response[0])
        assert result == {
            u'status': u'ok',
            u'data': [
                {
                    u'id': 1,
                    u'title': u'атлантида (АУ)',
                    u'slug': u'slug-2',
                    u'popularity': 10,
                },
                {
                    u'id': 1,
                    u'title': u'аэрофлот (ZZ)',
                    u'slug': u'slug-1',
                    u'popularity': 10,
                },
                {
                    u'id': 1,
                    u'title': u'авиалинии',
                    u'slug': u'slug-3',
                    u'popularity': 1,
                },
                {
                    u'id': 1,
                    u'title': u'not popular',
                    u'slug': u'slug-4',
                    u'popularity': 0,
                },
            ]
        }

        result = self._view._unsafe_process({
            'national_version': u'ua',
            'lang': u'ru',
            'count': 10,
        })

        result = ujson.loads(result.response[0])
        assert result == {
            u'status': u'ok',
            u'data': [
                {
                    u'id': 1,
                    u'title': u'not popular',
                    u'slug': u'slug-4',
                    u'popularity': 15,
                },
                {
                    u'id': 1,
                    u'title': u'авиалинии',
                    u'slug': u'slug-3',
                    u'popularity': 10,
                },
                {
                    u'id': 1,
                    u'title': u'аэрофлот (ZZ)',
                    u'slug': u'slug-1',
                    u'popularity': 5,
                },
                {
                    u'id': 1,
                    u'title': u'атлантида (АУ)',
                    u'slug': u'slug-2',
                    u'popularity': 1,
                },
            ]
        }
