from __future__ import absolute_import

from datetime import datetime
from mock import Mock

from travel.avia.library.python.common.models.geo import CityMajority
from travel.avia.library.python.common.utils.date import MSK_TZ, get_pytz
from travel.avia.backend.repository.settlement import SettlementRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.tester.factories import create_settlement, create_translated_title, \
    create_country, create_region
from travel.avia.library.python.tester.testcase import TestCase


class SettlementRepositoryTest(TestCase):
    def setUp(self):
        self._translated_title_repository = TranslatedTitleRepository()

        self._environment = Mock()
        self._environment.now_aware = Mock(
            return_value=MSK_TZ.localize(datetime(2017, 9, 1))
        )
        self._repo = SettlementRepository(
            translated_title_repository=self._translated_title_repository,
            environment=self._environment
        )

    def test_some(self):
        country = create_country()
        region = create_region()
        title = create_translated_title(
            ru_nominative='ru_nominative'
        )
        s = create_settlement(
            new_L_title_id=title.id,
            iata='some-iata',
            sirena_id='some-sirena',
            _geo_id=1,
            country=country,
            region=region,
            _disputed_territory=True,
            majority=CityMajority.CAPITAL_ID,
            time_zone="MSK",
            latitude=123,
            longitude=321
        )

        self._repo.pre_cache()
        m = self._repo.get(
            s.id
        )

        assert m is not None
        assert m.pk == s.id
        assert m.point_key == u'c{}'.format(s.id)
        assert m.get_title('ru') == 'ru_nominative'
        assert m.iata == 'some-iata'
        assert m.sirena == 'some-sirena'
        assert m.geo_id == 1
        assert m.country_id == country.id
        assert m.region_id == region.id
        assert m.is_disputed_territory is True
        assert m.majority_id == CityMajority.CAPITAL_ID
        assert m.pytz == get_pytz('MSK')
        assert m.utcoffset == 3 * 60 * 60
        assert m.latitude == 123
        assert m.longitude == 321
