# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.geo import Settlement, Country, Region, District
from common.tester.testcase import TestCase
from common.utils.geobase import geobase
from common.utils.settlement import get_ymap_url


class TestSettlementYmapUrl(TestCase):
    def setUp(self):
        self.russia = Country.objects.get(id=Country.RUSSIA_ID)

    def test_ymap_url_with_geobase(self):
        region = Region(id=Region.MOSCOW_REGION_ID, _geo_id=1, title='Москва и Московская область', country=self.russia)
        district = District(title='Воскресенский', region=region)
        settlement = Settlement(_geo_id=215, title='Дубна', country=self.russia, region=region, district=district)

        ymap_url_ru = get_ymap_url(settlement, 'ru', 'ru')
        ymap_url_ua = get_ymap_url(settlement, 'ua', 'uk')

        geo_region = geobase.region_by_id(215)

        assert ymap_url_ru == 'https://maps.yandex.ru/215/{}/'.format(geo_region.ename.lower())
        assert ymap_url_ua == 'https://maps.yandex.ua/215/{}/'.format(geo_region.ename.lower())

    def test_ymap_url_with_geobase_turkish_letters(self):
        settlement = Settlement(_geo_id=115792)

        class GeoObject(object):
            id = 115792
            ename = 'Ağın'.encode('utf-8')  # не юникодная строка. g "с крышкой", i "без точки"

        with mock.patch.object(Settlement, 'get_geobase_region', return_value=GeoObject()):
            ymap_url_ru = get_ymap_url(settlement, 'ru', 'ru')

        assert ymap_url_ru == 'https://maps.yandex.ru/115792/a%C4%9F%C4%B1n/'

    def test_ymap_url_without_geobase(self):
        region = Region(id=1, _geo_id=1, title='Москва и Московская область', country=self.russia)
        district = District(id=2, title='Воскресенский', region=region)
        settlement = Settlement(id=29047, title='Имени Цюрупы', country=self.russia, region=region, district=district)

        with mock.patch.object(Settlement, 'L_title_with_full_geography', return_value='им. tsuryupi, Russia') as \
                m_title_with_full_geography:
            ymap_url_ru = get_ymap_url(settlement, 'ru', 'ru')
            ymap_url_ua = get_ymap_url(settlement, 'ua', 'uk')

            assert ymap_url_ru == 'https://maps.yandex.ru/?text=%D0%B8%D0%BC.+tsuryupi%2C+Russia'
            assert ymap_url_ua == 'https://maps.yandex.ua/?text=%D0%B8%D0%BC.+tsuryupi%2C+Russia'

        assert m_title_with_full_geography.call_count == 2
        m_title_with_full_geography.assert_any_call('ru', 'ru')
        m_title_with_full_geography.assert_any_call('ua', 'uk')
