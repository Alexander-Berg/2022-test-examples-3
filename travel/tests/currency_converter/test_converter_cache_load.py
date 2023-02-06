# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.models.currency.currency_converter import ConverterCache


@mock.patch('django.core.cache.cache.get')
@mock.patch('travel.rasp.library.python.common23.models.currency.currency_converter.ConverterCache._get_key',
            side_effect=lambda national_version: national_version + ' key')
class TestConverterCacheLoad(TestCase):
    """
    Тесты на функцию ConverterCache.load()
    """
    def test_load_ru(self, m_get_key, m_cache_get):
        """
        Читаем из кэша курсы валют для русской национальной версии.
        """
        rur_cache = ConverterCache('rur rates', 'rur actual datetime', 'ru')
        m_cache_get.return_value = rur_cache

        assert ConverterCache.load('ru') == rur_cache
        m_cache_get.assert_called_once_with('ru key')

    def test_load_ua(self, m_get_key, m_cache_get):
        """
        Читаем из кэша курсы валют для украинской национальной версии.
        """
        uah_cache = ConverterCache('uah rates', 'uah actual datetime', 'ua')
        m_cache_get.return_value = uah_cache

        assert ConverterCache.load('ua') == uah_cache
        m_cache_get.assert_called_once_with('ua key')

    def test_load_default(self, m_get_key, m_cache_get):
        """
        Читаем из кэша курсы валют для дефолтной национальной версии.
        """
        rur_cache = ConverterCache('rur rates', 'rur actual datetime', 'ru')
        m_cache_get.return_value = rur_cache

        assert ConverterCache.load() == rur_cache
        m_cache_get.assert_called_once_with('ru key')

    def test_load_ru_no_cache(self, m_get_key, m_cache_get):
        """
        Читаем из кэша курсы валют для русской национальной версии. В кэше данных не оказалось.
        """
        m_cache_get.return_value = None

        assert ConverterCache.load('ru') is None
        m_cache_get.assert_called_once_with('ru key')
