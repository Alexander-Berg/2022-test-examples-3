# -*- coding: utf-8 -*-
import mock

from travel.avia.library.python.common.models.geo import Settlement, Region, District
from travel.avia.library.python.common.utils.settlement import get_ymap_url
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_country


class TestSettlementYmapUrl(TestCase):
    def setUp(self):
        self.russia = create_country()

    def test_ymap_url_with_geobase(self):
        region = Region(id=Region.MOSCOW_REGION_ID, _geo_id=1, title=u'Москва и Московская область', country=self.russia)
        district = District(title=u'Воскресенский', region=region)
        settlement = Settlement(_geo_id=111, title=u'Имени Цюрупы', country=self.russia, region=region, district=district)

        class GeoObject(object):
            id = 111
            ename = 'Posyolok Imeni Tsyurupy'

        with mock.patch.object(Settlement, 'get_geobase_region', return_value=GeoObject()):
            ymap_url_ru = get_ymap_url(settlement, u'ru', u'ru')
            ymap_url_ua = get_ymap_url(settlement, u'ua', u'uk')

        assert ymap_url_ru == u'https://maps.yandex.ru/111/posyolok-imeni-tsyurupy/'
        assert ymap_url_ua == u'https://maps.yandex.ua/111/posyolok-imeni-tsyurupy/'

    def test_ymap_url_with_geobase_turkish_letters(self):
        settlement = Settlement(_geo_id=115792)

        class GeoObject(object):
            id = 115792
            ename = 'Ağın'  # не юникодная строка. g "с крышкой", i "без точки"

        with mock.patch.object(Settlement, 'get_geobase_region', return_value=GeoObject()):
            ymap_url_ru = get_ymap_url(settlement, u'ru', u'ru')

        assert ymap_url_ru == u'https://maps.yandex.ru/115792/a%C4%9F%C4%B1n/'

    def test_ymap_url_without_geobase(self):
        region = Region(id=1, _geo_id=1, title=u'Москва и Московская область', country=self.russia)
        district = District(id=2, title=u'Воскресенский', region=region)
        settlement = Settlement(id=29047, title=u'Имени Цюрупы', country=self.russia, region=region, district=district)

        with mock.patch.object(Settlement, 'L_title_with_full_geography', return_value='им. tsuryupi, Russia') as m_title_with_full_geography:
            ymap_url_ru = get_ymap_url(settlement, u'ru', u'ru')
            ymap_url_ua = get_ymap_url(settlement, u'ua', u'uk')

            assert ymap_url_ru == u'https://maps.yandex.ru/?text=%D0%B8%D0%BC.+tsuryupi%2C+Russia'
            assert ymap_url_ua == u'https://maps.yandex.ua/?text=%D0%B8%D0%BC.+tsuryupi%2C+Russia'

        assert m_title_with_full_geography.call_count == 2
        m_title_with_full_geography.assert_any_call(u'ru', u'ru')
        m_title_with_full_geography.assert_any_call(u'ua', u'uk')
