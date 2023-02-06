# coding=utf-8
from __future__ import absolute_import

from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.geo.country import CountriesView, CountryView
from travel.avia.backend.repository.country import CountryRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.tester.factories import create_translated_title, create_country


class CountryViewsTest(TestCase):
    DEFAULT_TITLE = 'ru_nominative'

    def setUp(self):
        self._translated_title_repository = TranslatedTitleRepository()
        self.repo = CountryRepository(self._translated_title_repository)

        self.country_view = CountryView(
            country_repository=self.repo,
            logger=cast(Logger, Mock()),
        )
        self.countries_view = CountriesView(
            country_repository=self.repo,
            logger=cast(Logger, Mock()),
        )

    def create_country(self, **kwargs):
        title = create_translated_title(ru_nominative=self.DEFAULT_TITLE)
        return create_country(new_L_title_id=title.id, **kwargs)

    def test_country_view(self):
        test_id, test_code, test_geo_id = 666, 'AA', 123
        expected = {
            'code': test_code,
            'id': test_id,
            'point_key': u'l%d' % test_id,
            'title': self.DEFAULT_TITLE,
            'geo_id': test_geo_id,
        }
        self.create_country(
            id=test_id,
            code=test_code,
            _geo_id=test_geo_id,
        )
        self.repo.pre_cache()

        actual = self.country_view._process({
            'country_id': test_id,
            'lang': u'ru',
            'fields': {},
        })
        assert actual == expected

    def test_countries_view(self):
        test_info = {666: ('AA', 123), 667: ('AB', 456)}
        expected = {
            _id: {
                'code': code,
                'id': _id,
                'point_key': u'l%d' % _id,
                'title': self.DEFAULT_TITLE,
                'geo_id': geo_id,
            } for _id, (code, geo_id) in test_info.iteritems()
        }

        for _id, (code, geo_id) in test_info.iteritems():
            self.create_country(
                id=_id,
                code=code,
                _geo_id=geo_id,
            )
        self.repo.pre_cache()

        actual = self.countries_view._process({
            'lang': u'ru',
            'fields': {},
        })

        for _id in expected:
            assert _id in actual
            assert actual[_id] == expected[_id]
