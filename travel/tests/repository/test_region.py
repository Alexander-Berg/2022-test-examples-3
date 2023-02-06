# -*- encoding: utf-8 -*-
from __future__ import absolute_import

from datetime import datetime

from mock import Mock

from travel.avia.library.python.common.utils.date import MSK_TZ
from travel.avia.backend.repository.region import RegionRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.tester.factories import (
    create_translated_title,
    create_region,
    create_country)
from travel.avia.library.python.tester.testcase import TestCase


class RegionRepositoryTest(TestCase):
    def setUp(self):
        self._translated_title_repository = TranslatedTitleRepository()

        self._environment = Mock()
        self._environment.now_aware = Mock(
            return_value=MSK_TZ.localize(datetime(2017, 9, 1))
        )
        self._repo = RegionRepository(
            translated_title_repository=self._translated_title_repository
        )

    def test_some(self):
        title = create_translated_title(
            ru_nominative='ru_nominative'
        )
        c = create_country(_geo_id=7, title=u'Test', title_ru=u'Тест')
        r = create_region(
            new_L_title_id=title.id,
            _geo_id=213,
            country=c
        )

        self._repo.pre_cache()
        m = self._repo.get(
            r.id
        )

        assert m.pk == r.id
        assert m.point_key == u'r{}'.format(r.id)
        assert m.get_title('ru') == 'ru_nominative'
        assert m.geo_id == r._geo_id
        assert m.country_id == c.id
