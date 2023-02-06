# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement import get_visible_nearest_capital


class TestGetVisibleNearestCapital(TestCase):
    def test_district_capital(self):
        """
        Найдена столица района.
        """
        settlement = create_settlement()
        capital = create_settlement()

        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_district_capital',
                        return_value=capital) as m_district_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_region_capital') as m_region_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_country_capital') as m_country_capital:

            assert get_visible_nearest_capital(settlement, u'ua') == capital

        m_district_capital.assert_called_once_with(settlement)
        m_region_capital.assert_not_called()
        m_country_capital.assert_not_called()

    def test_region_capital(self):
        """
        Стлица района не найдена. Найдена столица региона.
        """
        settlement = create_settlement()
        capital = create_settlement()

        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_district_capital',
                        return_value=None) as m_district_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_region_capital',
                           return_value=capital) as m_region_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_country_capital') as m_country_capital:

            assert get_visible_nearest_capital(settlement, u'ua') == capital

        m_district_capital.assert_called_once_with(settlement)
        m_region_capital.assert_called_once_with(settlement)
        m_country_capital.assert_not_called()

    def test_country_capital(self):
        """
        Столицы района и региона не найдены. Найдена столица страны.
        """
        settlement = create_settlement()
        capital = create_settlement()

        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_district_capital',
                        return_value=None) as m_district_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_region_capital',
                           return_value=None) as m_region_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_country_capital',
                           return_value=capital) as m_country_capital:

            assert get_visible_nearest_capital(settlement, u'ua') == capital

        m_district_capital.assert_called_once_with(settlement)
        m_region_capital.assert_called_once_with(settlement)
        m_country_capital.assert_called_once_with(settlement, u'ua')

    def test_no_capital(self):
        """
        Стлицы района, региона и страны не найдены.
        """
        settlement = create_settlement()

        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_district_capital',
                        return_value=None) as m_district_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_region_capital',
                           return_value=None) as m_region_capital, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_country_capital',
                           return_value=None) as m_country_capital:

            assert get_visible_nearest_capital(settlement, u'ua') is None

        m_district_capital.assert_called_once_with(settlement)
        m_region_capital.assert_called_once_with(settlement)
        m_country_capital.assert_called_once_with(settlement, u'ua')
