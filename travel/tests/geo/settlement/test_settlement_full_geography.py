# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.models.core.geo.settlement import Settlement
from travel.rasp.library.python.common23.models.core.geo.country import Country
from travel.rasp.library.python.common23.models.core.geo.region import Region
from travel.rasp.library.python.common23.models.core.geo.district import District
from travel.rasp.library.python.common23.tester.factories import create_country
from travel.rasp.library.python.common23.tester.testcase import TestCase


class TestSettlementFullGeography(TestCase):
    def setUp(self):
        self.ukraine = create_country(id=Country.UKRAINE_ID, _geo_id=Country.UKRAINE_ID, title=u'Украина',
                                      title_ru=u'Украина', title_uk=u'Україна')
        self.russia = Country.objects.get(id=Country.RUSSIA_ID)

    def test_title_with_full_geography_kiev(self):
        region = Region(id=20544, _geo_id=20544, title_ru=u'Киев и Киевская область',
                        title_uk=u'Київ і Київська область', country=self.ukraine)
        settlement = Settlement(id=Settlement.KIEV_ID, _geo_id=Settlement.KIEV_ID, title_ru=u'Киев',
                                title_uk=u'Київ', country=self.ukraine, region=region)

        title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')
        title_ua_ru = settlement.L_title_with_full_geography('ua', 'ru')
        title_ua_uk = settlement.L_title_with_full_geography('ua', 'uk')

        assert title_ru_ru == u'Киев, Украина'
        assert title_ua_ru == u'Киев, Украина'
        assert title_ua_uk == u'Київ, Україна'

    def test_title_with_full_geography_sevastopol(self):
        region = Region(id=977, _geo_id=977, title_ru=u'Крым', title_uk=u'Крим', country=self.russia,
                        disputed_territory=True)
        settlement = Settlement(id=959, _geo_id=959, title_ru=u'Севастополь', title_uk=u'Севастополь',
                                country=self.russia, region=region)

        title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')
        title_ua_ru = settlement.L_title_with_full_geography('ua', 'ru')
        title_ua_uk = settlement.L_title_with_full_geography('ua', 'uk')
        title_uk = settlement.L_title_with_full_geography(lang='uk')
        title_ru = settlement.L_title_with_full_geography(lang='ru')

        assert title_ru_ru == u'Севастополь, Крым, Россия'
        assert title_ua_ru == u'Севастополь, Крым, Украина'
        assert title_ua_uk == u'Севастополь, Крим, Україна'
        assert title_ru == u'Севастополь, Крым'
        assert title_uk == u'Севастополь, Крим'

    def test_title_with_full_geography_volokolamsk(self):
        region = Region(id=1, _geo_id=1, title=u'Москва и Московская область', country=self.russia)
        district = District(id=1, title=u'Волоколамский', region=region)
        settlement = Settlement(id=10721, _geo_id=10721, title=u'Волоколамск', country=self.russia, region=region,
                                district=district)

        title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')

        assert title_ru_ru == u'Волоколамск, Волоколамский р-н, Москва и Московская область, Россия'

    def test_title_with_full_geography_empty_geography(self):
        settlement = Settlement(title=u'Волоколамск')

        title_ru_ru = settlement.L_title_with_full_geography('ru', 'ru')

        assert title_ru_ru == u'Волоколамск'
