# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from hamcrest import assert_that, has_entries

from common.models.geo import Settlement
from common.tester.factories import create_settlement, create_region
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement import settlement_json


class TestSetlementJson(TestCase):
    def setUp(self):
        self.region_id = 888
        self.region = create_region(id=self.region_id)

        self.settlement_id = 777
        self.settlement = create_settlement(id=self.settlement_id, slug='sverdlovsk-44',
                                            title='Свердловск-44', title_ru_genitive='Свердловска-44',
                                            region=self.region)
        self.timezone = 'Asia/Yekaterinburg'
        self.title_with_geography = 'Свердловск-44, Свердловская область, Россия'
        self.ymap_url = 'https://maps.yandex.ru/9999/novouralsk'
        self.suburban_zone_json = 'suburban zone json'

    def test_settlement_json(self):
        with mock.patch.object(Settlement, 'get_tz_name', return_value=self.timezone) as m_get_tz_name, \
                mock.patch.object(Settlement, 'L_title_with_full_geography',
                                  return_value=self.title_with_geography) as m_title_with_full_geography, \
                mock.patch('common.utils.settlement.get_ymap_url', return_value=self.ymap_url) as m_get_ymap_url, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_suburban_zone_json',
                           return_value=self.suburban_zone_json) as m_suburban_zone_json:

            data = settlement_json(self.settlement, 'ua', 'uk')

            assert data == {
                'id': self.settlement_id,
                'slug': 'sverdlovsk-44',
                'title': 'Свердловск-44',  # город по-украински - місто
                'title_with_type': 'м. Свердловск-44',  # город по-украински - місто
                'title_genitive': None,
                'timezone': self.timezone,
                'blablacar_title': self.title_with_geography,
                'ymap_url': self.ymap_url,
                'region_id': self.region_id,
                'suburban_zone': self.suburban_zone_json,
                'key': 'c{}'.format(self.settlement_id)
            }

        m_get_tz_name.assert_called_once_with()
        m_title_with_full_geography.assert_called_once_with('ua', 'uk')
        m_get_ymap_url.assert_called_once_with(self.settlement, 'ua', 'uk')
        m_suburban_zone_json.assert_called_once_with(self.settlement)

    def test_settlement_json_ru(self):
        data = settlement_json(self.settlement, 'ru', 'ru')

        assert_that(data, has_entries({
            'title': 'Свердловск-44',
            'title_with_type': 'г. Свердловск-44',  # город по-украински - місто
            'title_genitive': 'Свердловска-44',
        }))
