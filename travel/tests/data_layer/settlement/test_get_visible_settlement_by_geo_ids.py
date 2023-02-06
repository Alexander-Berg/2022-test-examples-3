# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.geo import CityMajority
from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.train_api.data_layer.settlement import get_visible_nearest_settlement_by_geo_ids


class TestGetVisibleSettlementByGeoIds(TestCase):
    def test_first_found(self):
        """
        Первый город из списка найден и не скрыт.
        """
        geo_id = 777
        settlement = create_settlement(_geo_id=geo_id)
        result = get_visible_nearest_settlement_by_geo_ids([geo_id, 888, 999], u'ua')
        assert result == settlement

    def test_first_hidden_second_found(self):
        """
        Первый город из списка найден, но он скрыт. Найден второй город из списка.
        """
        geo_id1 = 777
        create_settlement(_geo_id=geo_id1, hidden=True)

        geo_id2 = 888
        settlement2 = create_settlement(_geo_id=geo_id2)
        result = get_visible_nearest_settlement_by_geo_ids([geo_id1, geo_id2, 999], u'ua')
        assert result == settlement2

    def test_first_not_found_second_found(self):
        """
        Первый город из списка не найден. Найден второй город из списка.
        """
        _geo_id2 = 888
        settlement2 = create_settlement(_geo_id=_geo_id2)
        result = get_visible_nearest_settlement_by_geo_ids([777, _geo_id2, 999], u'ua')
        assert result == settlement2

    def test_first_hidden_district_capital_found(self):
        """
        Первый город из списка найден, но он скрыт.
        Найдена столица райцентра (отсутствует в преданном списке geo_ids).
        """
        district_capital = create_settlement(title=u'Новая Ляля', majority=CityMajority.COMMON_CITY_ID)

        geo_id1 = 777
        settlement1 = create_settlement(_geo_id=geo_id1, hidden=True, title=u'Лобва',
                                        majority=CityMajority.COMMON_CITY_ID)

        geo_id2 = 888
        create_settlement(_geo_id=geo_id2, title=u'Екатеринбург', majority=CityMajority.REGION_CAPITAL_ID)

        with mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_district_capital',
                        return_value=district_capital) as m_get_district_capital:
            result = get_visible_nearest_settlement_by_geo_ids([geo_id1, geo_id2, 999], u'ua')
            assert result == district_capital

        m_get_district_capital.assert_called_once_with(settlement1)

    def test_first_hidden_second_hidden_region_capital_found(self):
        """
        Первый и второй города из списка найдены, но скрыты.
        Найдена столица региона (отсутствует в преданном списке geo_ids).
        """
        geo_id1 = 777
        create_settlement(_geo_id=geo_id1, title=u'Лобва', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id2 = 888
        settlement2 = create_settlement(_geo_id=geo_id2, title=u'Новая Ляля', hidden=True,
                                        majority=CityMajority.COMMON_CITY_ID)

        region_capital = create_settlement(title=u'Екатеринбург')

        with mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_region_capital',
                        return_value=region_capital) as m_get_visible_region_capital:
            result = get_visible_nearest_settlement_by_geo_ids([777, geo_id2], u'ua')
            assert result == region_capital

        m_get_visible_region_capital.assert_called_once_with(settlement2)

    def test_first_hidden_second_hidden_country_capital_found(self):
        """
        Первый и второй города из списка найдены, но скрыты.
        Столица региона не найдена. Найдена столица страны (отсутствует в преданном списке geo_ids).
        """
        geo_id1 = 777
        create_settlement(_geo_id=geo_id1, title=u'Лобва', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id2 = 888
        settlement2 = create_settlement(_geo_id=geo_id2, title=u'Новая Ляля', hidden=True,
                                        majority=CityMajority.COMMON_CITY_ID)

        country_capital = create_settlement(title=u'Москва')

        with mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_region_capital',
                        return_value=None) as m_get_visible_region_capital, \
                mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_country_capital',
                           return_value=country_capital) as m_get_visible_country_capital:
            result = get_visible_nearest_settlement_by_geo_ids([geo_id1, geo_id2], u'ua')
            assert result == country_capital

        m_get_visible_region_capital.assert_called_once_with(settlement2)
        m_get_visible_country_capital.assert_called_once_with(settlement2, u'ua')

    def test_first_hidden_second_hidden_region_capital_hidden_country_capital_found(self):
        """
        Первый, второй и третий (столица региона) города из списка найдены, но скрыты.
        Найдена столица страны (отсутствует в преданном списке geo_ids).
        """
        geo_id1 = 777
        create_settlement(_geo_id=geo_id1, title=u'Лобва', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id2 = 888
        create_settlement(_geo_id=geo_id2, title=u'Новая Ляля', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id3 = 999
        settlement3 = create_settlement(_geo_id=geo_id3, title=u'Екатеринбург', hidden=True,
                                        majority=CityMajority.REGION_CAPITAL_ID)

        country_capital = create_settlement(title=u'Москва')

        with mock.patch('travel.rasp.train_api.data_layer.settlement.'
                        'get_visible_region_capital') as m_get_visible_region_capital, \
                mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_country_capital',
                           return_value=country_capital) as m_get_visible_country_capital:
            result = get_visible_nearest_settlement_by_geo_ids([geo_id1, geo_id2, geo_id3], u'ua')
            assert result == country_capital

        m_get_visible_region_capital.assert_not_called()
        m_get_visible_country_capital.assert_called_once_with(settlement3, u'ua')

    def test_first_hidden_second_hidden_region_capital_not_found_country_capital_found(self):
        """
        Первый и второй города из списка найдены, но скрыты.
        Столица региона не найдена.
        Найдена столица страны (отсутствует в преданном списке geo_ids).
        """
        geo_id1 = 777
        create_settlement(_geo_id=geo_id1, title=u'Лобва', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id2 = 888
        settlement2 = create_settlement(_geo_id=geo_id2, title=u'Новая Ляля', hidden=True,
                                        majority=CityMajority.COMMON_CITY_ID)

        country_capital = create_settlement(title=u'Москва')

        with mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_region_capital',
                        return_value=None) as m_get_visible_region_capital, \
                mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_country_capital',
                           return_value=country_capital) as m_get_visible_country_capital:
            result = get_visible_nearest_settlement_by_geo_ids([geo_id1, geo_id2], u'ua')
            assert result == country_capital

        m_get_visible_region_capital.assert_called_once_with(settlement2)
        m_get_visible_country_capital.assert_called_once_with(settlement2, u'ua')

    def test_first_hidden_second_hidden_region_capital_hidden_country_capital_hidden(self):
        """
        Первый, второй, третий (столица региона) и четвертый (стлица страны) города из списка найдены, но скрыты.
        """
        geo_id1 = 777
        create_settlement(_geo_id=geo_id1, title=u'Лобва', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id2 = 888
        create_settlement(_geo_id=geo_id2, title=u'Новая Ляля', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id3 = 999
        create_settlement(_geo_id=geo_id3, title=u'Екатеринбург', hidden=True, majority=CityMajority.REGION_CAPITAL_ID)

        geo_id4 = 1111
        create_settlement(_geo_id=geo_id4, title=u'Москва', hidden=True, majority=CityMajority.CAPITAL_ID)

        with mock.patch('travel.rasp.train_api.data_layer.settlement.'
                        'get_visible_region_capital') as m_get_visible_region_capital, \
                mock.patch('travel.rasp.train_api.data_layer.settlement.'
                           'get_visible_country_capital') as m_get_visible_country_capital:
            result = get_visible_nearest_settlement_by_geo_ids([geo_id1, geo_id2, geo_id3, geo_id4], u'ua')
            assert result is None

        m_get_visible_region_capital.assert_not_called()
        m_get_visible_country_capital.assert_not_called()

    def test_first_hidden_second_hidden_region_capital_not_found_country_capital_not_found(self):
        """
        Первый и второй города из списка найдены, но скрыты.
        Столица региона не найдена.
        Столица страны не найдена.
        """
        geo_id1 = 777
        create_settlement(_geo_id=geo_id1, title=u'Лобва', hidden=True, majority=CityMajority.COMMON_CITY_ID)

        geo_id2 = 888
        settlement2 = create_settlement(_geo_id=geo_id2, title=u'Новая Ляля', hidden=True,
                                        majority=CityMajority.COMMON_CITY_ID)

        with mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_region_capital',
                        return_value=None) as m_get_visible_region_capital, \
                mock.patch('travel.rasp.train_api.data_layer.settlement.get_visible_country_capital',
                           return_value=None) as m_get_visible_country_capital:
            result = get_visible_nearest_settlement_by_geo_ids([geo_id1, geo_id2], u'ua')
            assert result is None

        m_get_visible_region_capital.assert_called_once_with(settlement2)
        m_get_visible_country_capital.assert_called_once_with(settlement2, u'ua')
