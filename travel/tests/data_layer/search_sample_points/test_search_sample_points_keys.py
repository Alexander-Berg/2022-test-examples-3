# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.geo import Country
from common.tester.testcase import TestCase
from travel.rasp.train_api.data_layer.search_sample_points import get_search_sample_points_keys


@mock.patch('travel.rasp.train_api.data_layer.search_sample_points.get_country_id_for_search_sample_points')
class TestSearchSamplePointsKeys(TestCase):
    def setUp(self):
        super(TestSearchSamplePointsKeys, self).setUp()
        self.settlement_id = 888

    def test_ru(self, m_get_country_id):
        m_get_country_id.return_value = Country.RUSSIA_ID

        assert get_search_sample_points_keys(self.settlement_id, 'ru') == ('c213', 'c2')  # Москва - СПб
        m_get_country_id.assert_called_once_with(self.settlement_id, 'ru')

    def test_ua(self, m_get_country_id):
        m_get_country_id.return_value = Country.UKRAINE_ID

        assert get_search_sample_points_keys(self.settlement_id, 'ua') == ('c143', 'c144')  # Киев - Львов
        m_get_country_id.assert_called_once_with(self.settlement_id, 'ua')
