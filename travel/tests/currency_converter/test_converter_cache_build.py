# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import datetime

import mock
from freezegun import freeze_time

from travel.rasp.library.python.common23.tester.factories import create_currency
from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.models.currency.currency_converter import ConverterCache

NATIONAL_CURRENCY_RATES_GEO_ID = {
    'ru': 'msk_geoid',
    'ua': 'kiev_geoid',
    'tr': 'ist_geoid',
}

NATIONAL_CURRENCIES = {
    'ru': 'RUR',
    'ua': 'UAH',
    'tr': 'TRY'
}

RATES = 'currency rates'
TEST_NOW = datetime.datetime(2016, 3, 9, 13, 10, 30)


@mock.patch('travel.rasp.library.python.common23.models.currency.currency.Currency.fetch_rates',
            return_value=('rates source', RATES))
@mock.patch('travel.rasp.library.python.common23.models.currency.currency_converter.ConverterCache.get_national_currency',
            side_effect=lambda national_version: NATIONAL_CURRENCIES[national_version])
@mock.patch('travel.rasp.library.python.common23.models.currency.currency_converter.ConverterCache.get_currency_geoid',
            side_effect=lambda national_version: NATIONAL_CURRENCY_RATES_GEO_ID[national_version])
@freeze_time(TEST_NOW)
class TestConverterCacheBuild(TestCase):
    """
    Тесты на функцию ConverterCache.build()
    """
    def setUp(self):
        super(TestConverterCacheBuild, self).setUp()

        self.rur = create_currency(code='RUR', name='рубли')
        self.uah = create_currency(code='UAH', name='гривны')
        self.lira = create_currency(code='TRY', name='лиры')

    def test_build_default_national_version(self, m_patch_get_currency_geoid, m_get_national_currency, m_fetch_rates):
        """
        Строим кэш, явно не указывая национальную версию.
        """
        cache = ConverterCache.build()
        self._assert_cache(cache, 'ru')
        self._assert_mock_calls(m_patch_get_currency_geoid, m_get_national_currency, m_fetch_rates,
                                'ru', 'RUR', 'msk_geoid')

    def test_build_ru(self, m_patch_get_currency_geoid, m_get_national_currency, m_fetch_rates):
        """
        Строим кэш для русской национальной версии.
        """
        cache = ConverterCache.build('ru')
        self._assert_cache(cache, 'ru')
        self._assert_mock_calls(m_patch_get_currency_geoid, m_get_national_currency, m_fetch_rates,
                                'ru', 'RUR', 'msk_geoid')

    def test_build_ua(self, m_patch_get_currency_geoid, m_get_national_currency, m_fetch_rates):
        """
        Строим кэш для украинской национальной версии.
        """
        cache = ConverterCache.build('ua')
        self._assert_cache(cache, 'ua')
        self._assert_mock_calls(m_patch_get_currency_geoid, m_get_national_currency, m_fetch_rates,
                                'ua', 'UAH', 'kiev_geoid')

    def _assert_cache(self, cache, national_version):
        assert cache.rates == RATES
        assert cache.actual_datetime == TEST_NOW
        assert cache.national_version == national_version

    def _assert_mock_calls(self, m_patch_get_currency_geoid, m_get_national_currency, m_fetch_rates,
                           national_version, base_currency, currency_geoid):
        m_fetch_rates.assert_called_once_with(mock.ANY, currency_geoid, base_currency=base_currency)
        assert set(m_fetch_rates.call_args[0][0]) == {self.rur, self.uah, self.lira}

        m_patch_get_currency_geoid.assert_called_once_with(national_version)
        m_get_national_currency.assert_called_once_with(national_version)
