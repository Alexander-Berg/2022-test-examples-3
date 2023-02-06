# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.tester.factories import create_settlement, create_country
from common.tester.testcase import TestCase

from travel.rasp.library.python.common23.models.core.geo.city_majority import CityMajority
from travel.rasp.library.python.common23.models.core.geo.country import Country, get_country_capital
from travel.rasp.library.python.common23.models.core.geo.settlement import Settlement

from travel.rasp.train_api.data_layer.settlement import get_visible_country_capital


@mock.patch('travel.rasp.library.python.common23.models.core.geo.country.get_country_capital',
            side_effect=get_country_capital.__wrapped__)
class TestGetVisibleCountryCapital(TestCase):
    def test_no_country(self, m_get_country_capital):
        """
        Населенный пункт не привязан ни к какой стране.
        """
        settlement = create_settlement(majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_country_capital(settlement, u'ua') is None

    def test_country_without_capital(self, m_get_country_capital):
        """
        Населенный пункт привязан к стране, у которой не определна столица.
        """
        country = create_country(id=Country.UKRAINE_ID)
        settlement = create_settlement(country=country, majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_country_capital(settlement, u'ua') is None

    def test_country_hidden_capital(self, m_get_country_capital):
        """
        Населенный пункт привязан к стране, у которого столица скрытая.
        """
        country = create_country(id=Country.UKRAINE_ID)
        create_settlement(country=country, majority=CityMajority.CAPITAL_ID, hidden=True)
        settlement = create_settlement(country=country, majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_country_capital(settlement, u'ua') is None

    def test_country_visible_capital(self, m_get_country_capital):
        """
        Населенный пункт привязан к стране, у которой столица видимая.
        """
        country = create_country(id=Country.UKRAINE_ID)
        country_capital = create_settlement(country=country, majority=CityMajority.CAPITAL_ID)
        settlement = create_settlement(country=country, majority=CityMajority.COMMON_CITY_ID)

        assert get_visible_country_capital(settlement, u'ua') == country_capital

    def test_country_visible_capital_sevastopol_ru(self, m_get_country_capital):
        """
        Столица страны для Севастополя, русская национальная версия.
        """
        settlement = create_settlement(_geo_id=959, majority=CityMajority.COMMON_CITY_ID, _disputed_territory=True)

        capital = get_visible_country_capital(settlement, u'ru')
        assert capital
        assert capital.id == Settlement.MOSCOW_ID

    def test_country_visible_capital_sevastopol_ua(self, m_get_country_capital):
        """
        Столица страны для Севастополя, украинская национальная версия.
        """
        country = create_country(id=Country.UKRAINE_ID, _geo_id=Country.UKRAINE_ID)
        country_capital = create_settlement(country=country, majority=CityMajority.CAPITAL_ID)
        settlement = create_settlement(_geo_id=959, majority=CityMajority.COMMON_CITY_ID, _disputed_territory=True)

        assert get_visible_country_capital(settlement, u'ua') == country_capital
