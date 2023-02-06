# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import mock

from travel.avia.library.python.common.utils.currency_converter import ConverterCache, CURRENCY_RATES_CACHE_TIMEOUT
from travel.avia.library.python.tester.testcase import TestCase


@mock.patch('django.core.cache.cache.set')
@mock.patch('travel.avia.library.python.common.utils.currency_converter.ConverterCache._get_key',
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
