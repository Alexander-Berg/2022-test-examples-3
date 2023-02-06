# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.models.currency.currency_converter import ConverterCache, CURRENCY_RATES_CACHE_TIMEOUT


@mock.patch('django.core.cache.cache.set')
@mock.patch('travel.rasp.library.python.common23.models.currency.currency_converter.ConverterCache._get_key',
            side_effect=lambda national_version: national_version + ' key')
class TestConverterCacheSave(TestCase):
    """
    Тесты на функцию ConverterCache.save()
    """
    def test_save_rur(self, m_get_key, m_cache_set):
        """
        Сохраняем в кэш курсы валют для рубля.
        """
        rur_cache = ConverterCache('rur rates', 'rur actual datetime', 'ru')
        rur_cache.save()

        m_cache_set.assert_called_once_with(key='ru key', value=rur_cache, timeout=CURRENCY_RATES_CACHE_TIMEOUT)

    def test_save_uah(self, m_get_key, m_cache_set):
        """
        Сохраняем в кэш курсы валют для гривны.
        """
        uah_cache = ConverterCache('uah rates', 'uah actual datetime', 'ua')
        uah_cache.save()

        m_cache_set.assert_called_once_with(key='ua key', value=uah_cache, timeout=CURRENCY_RATES_CACHE_TIMEOUT)
