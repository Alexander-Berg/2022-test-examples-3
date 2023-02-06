from __future__ import absolute_import

from datetime import datetime

from travel.avia.backend.repository.covid_info import CovidInfoRepository
from travel.avia.library.python.tester.factories import (
    create_translated_title,
    create_country,
    create_covid_info
)
from travel.avia.library.python.tester.testcase import TestCase


class CovidInfoRepositoryTest(TestCase):
    def setUp(self):
        self._repo = CovidInfoRepository()

    def test_some(self):
        title = create_translated_title(
            ru_nominative='ru_nominative'
        )
        c = create_country(
            new_L_title_id=title.id,
            _geo_id=1234567,
            code='XY',
        )

        ci = create_covid_info(
            pk=1,
            tourism=True,
            quarantine=True,
            quarantine_days=8,
            visa=False,
            avia=None,
            updated_at=datetime(2021, 7, 14),
            comment='test comment',
            country_id=c.id,
        )

        self._repo.pre_cache()
        m = self._repo.get(ci.country_id)

        assert m.pk == 1
        assert m.tourism is True
        assert m.quarantine is True
        assert m.quarantine_days == 8
        assert m.visa is False
        assert m.avia is None
        assert m.updated_at == datetime(2021, 7, 14)
        assert m.comment == 'test comment'
        assert m.country_id == c.id
