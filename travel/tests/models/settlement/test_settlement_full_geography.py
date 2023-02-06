# -*- coding: utf-8 -*-
from contextlib import contextmanager

import mock
from django.utils.translation import ugettext_lazy as _

from travel.avia.library.python.common.models.geo import Settlement, Country
from travel.avia.library.python.common.models_utils import geo
from travel.avia.library.python.tester.factories import (
    create_country, create_settlement,
    create_region, create_district,
    create_translated_title
)
from travel.avia.library.python.tester.testcase import TestCase


UKRAINE_RU = _(u'Ukraine RU')
UKRAINE_UK = _(u'Ukraine UK')
UKRAINE = _(u'Ukraine')
RUSSIA_GEO_ID = 225
UKRAINE_GEO_ID = 81234


@contextmanager
def geobase_mock(find_country_id=None):
    with mock.patch.object(geo, 'geobase') as _mock:
        if find_country_id is not None:
            _mock.find_country_id = mock.Mock(return_value=find_country_id)
        yield _mock


class TestSettlementFullGeography(TestCase):
    def setUp(self):
        self.ukraine_titles = create_translated_title(
            ru_nominative=UKRAINE_RU,
            uk_nominative=UKRAINE_UK,
        )
        self.ukraine = create_country(_geo_id=UKRAINE_GEO_ID, title=UKRAINE, new_L_title_id=self.ukraine_titles.id)

        if Country.objects.filter(_geo_id=RUSSIA_GEO_ID).count():
            self.russia = Country.objects.get(_geo_id=RUSSIA_GEO_ID)
        else:
            self.russia = create_country(_geo_id=RUSSIA_GEO_ID, title=u'Россия', title_ru=u'Россия')

    def test_title_with_full_geography_kiev(self):
        region_title = create_translated_title(
            ru_nominative=u'Киев и Киевская область',
            uk_nominative=u'Київ і Київська область'
        )
        region = create_region(id=20544, _geo_id=20544, new_L_title_id=region_title.id, country=self.ukraine)
        settlement_title = create_translated_title(
            ru_nominative=u'Киев',
            uk_nominative=u'Київ'
        )
        settlement = create_settlement(id=Settlement.KIEV_ID, _geo_id=Settlement.KIEV_ID,
                                       new_L_title_id=settlement_title.id, country=self.ukraine, region=region)

        title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')
        title_ua_ru = settlement.L_title_with_full_geography('ua', 'ru')
        title_ua_uk = settlement.L_title_with_full_geography('ua', 'uk')

        assert title_ru_ru == u'Киев, {}'.format(UKRAINE_RU)
        assert title_ua_ru == u'Киев, {}'.format(UKRAINE_RU)
        assert title_ua_uk == u'Київ, {}'.format(UKRAINE_UK)

    def test_title_with_full_geography_sevastopol(self):
        region_title = create_translated_title(
            ru_nominative=u'Крым',
            uk_nominative=u'Крим'
        )
        region = create_region(id=977, _geo_id=977, new_L_title_id=region_title.id, country=self.russia, disputed_territory=True)
        settlement_title = create_translated_title(
            ru_nominative=u'Севастополь',
            uk_nominative=u'Севастополь'
        )
        settlement = create_settlement(id=959, _geo_id=959, new_L_title_id=settlement_title.id, country=self.russia, region=region)

        with geobase_mock(find_country_id=RUSSIA_GEO_ID):
            title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')

        with geobase_mock(find_country_id=UKRAINE_GEO_ID):
            title_ua_ru = settlement.L_title_with_full_geography('ua', 'ru')
            title_ua_uk = settlement.L_title_with_full_geography('ua', 'uk')

        title_uk = settlement.L_title_with_full_geography(lang='uk')
        title_ru = settlement.L_title_with_full_geography(lang='ru')

        assert title_ru_ru == u'Севастополь, Крым, Россия'
        assert title_ua_ru == u'Севастополь, Крым, {}'.format(UKRAINE_RU)
        assert title_ua_uk == u'Севастополь, Крим, {}'.format(UKRAINE_UK)
        assert title_ru == u'Севастополь, Крым'
        assert title_uk == u'Севастополь, Крим'

    def test_title_with_full_geography_volokolamsk(self):
        region = create_region(id=1, _geo_id=1, title=u'Москва и Московская область', country=self.russia)
        district = create_district(id=1, title=u'Волоколамский', region=region)
        settlement = create_settlement(id=10721, _geo_id=10721, title=u'Волоколамск', country=self.russia, region=region, district=district)

        title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')

        assert title_ru_ru == u'Волоколамск, Волоколамский р-н, Москва и Московская область, Россия'

    def test_title_with_full_geography_empty_geography(self):
        settlement = create_settlement(title=u'Волоколамск')

        title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')

        assert title_ru_ru == u'Волоколамск'
