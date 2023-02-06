# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import Country
from common.tester.factories import create_settlement, create_country
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.data_layer.search_sample_points import (
    get_country_id_for_search_sample_points
)


class TestSearchSamplePointsKeys(TestCase):
    def setUp(self):
        super(TestSearchSamplePointsKeys, self).setUp()
        self.settlement_id = 888

    def test_ru_russian_city(self):
        create_settlement(id=self.settlement_id, country_id=Country.RUSSIA_ID)
        assert get_country_id_for_search_sample_points(self.settlement_id, 'ru') == Country.RUSSIA_ID

    def test_ru_ukrainian_city(self):
        create_country(id=Country.UKRAINE_ID)
        create_settlement(id=self.settlement_id, country_id=Country.UKRAINE_ID)
        assert get_country_id_for_search_sample_points(self.settlement_id, 'ru') == Country.UKRAINE_ID

    def test_ua_ukrainian_city(self):
        create_country(id=Country.UKRAINE_ID)
        create_settlement(id=self.settlement_id, country_id=Country.UKRAINE_ID)
        assert get_country_id_for_search_sample_points(self.settlement_id, 'ua') == Country.UKRAINE_ID

    def test_by_canadian_city(self):
        canada = create_country(id=95, title='Канада')
        create_settlement(id=self.settlement_id, country=canada)
        create_country(id=Country.BELARUS_ID, domain_zone='by')
        assert get_country_id_for_search_sample_points(self.settlement_id, 'by') == Country.BELARUS_ID

    def test_ru_sevastopol(self):
        create_settlement(id=self.settlement_id, _geo_id=959, country_id=Country.RUSSIA_ID,
                          _disputed_territory=True, title='Севастополь')
        assert get_country_id_for_search_sample_points(self.settlement_id, 'ru') == Country.RUSSIA_ID

    def test_ua_sevastopol(self):
        create_country(id=Country.UKRAINE_ID, _geo_id=Country.UKRAINE_ID)
        create_settlement(id=self.settlement_id, _geo_id=959, country_id=Country.RUSSIA_ID,
                          _disputed_territory=True, title='Севастополь')
        assert get_country_id_for_search_sample_points(self.settlement_id, 'ua') == Country.UKRAINE_ID

    def test_by_sevastopol(self):
        create_country(id=Country.UKRAINE_ID, _geo_id=Country.UKRAINE_ID)
        create_settlement(id=self.settlement_id, _geo_id=959, country_id=Country.RUSSIA_ID,
                          _disputed_territory=True, title='Севастополь')
        assert get_country_id_for_search_sample_points(self.settlement_id, 'by') == Country.UKRAINE_ID
