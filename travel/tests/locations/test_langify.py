# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.utils_db.locations import langify


class RequestStub(object):
    def __init__(self, national_version):
        self.NATIONAL_VERSION = national_version


@mock.patch('django.utils.translation.get_language')
@mock.patch('travel.rasp.library.python.common23.date.environment.get_request')
class TestLangify(TestCase):
    def test_ru(self, m_get_request, m_get_language):
        request = RequestStub('ru')
        m_get_request.return_value = request
        m_get_language.return_value = 'ru'

        url = '/thread/123?foo=bar&big=bone&foo=baz'
        langified = langify(url)

        assert langified == '/thread/123?foo=bar&big=bone&foo=baz'

    def test_ru_with_lang(self, m_get_request, m_get_language):
        request = RequestStub('ru')
        m_get_request.return_value = request
        m_get_language.return_value = 'ru'

        url = '/thread/123?foo=bar&big=bone&foo=baz&lang=ru'
        langified = langify(url)

        assert langified == '/thread/123?foo=bar&big=bone&foo=baz'

    def test_ua_uk(self, m_get_request, m_get_language):
        request = RequestStub('ua')
        m_get_request.return_value = request
        m_get_language.return_value = 'uk'

        url = '/thread/123?foo=bar&big=bone&foo=baz'
        langified = langify(url)

        assert langified == '/thread/123?foo=bar&big=bone&foo=baz'

    def test_ua_ru(self, m_get_request, m_get_language):
        request = RequestStub('ua')
        m_get_request.return_value = request
        m_get_language.return_value = 'ru'

        url = '/thread/123?foo=bar&big=bone&foo=baz'
        langified = langify(url)

        assert langified == '/thread/123?foo=bar&big=bone&foo=baz&lang=ru'

    def test_change_lang_ru_to_uk(self, m_get_request, m_get_language):
        request = RequestStub('ua')
        m_get_request.return_value = request

        url = '/thread/123?foo=bar&big=bone&foo=baz&lang=ru'
        langified = langify(url, 'uk')

        assert langified == '/thread/123?foo=bar&big=bone&foo=baz'
        m_get_language.assert_not_called()

    def test_national_version(self, m_get_request, m_get_language):
        """
        Построение URL'а для нацверсии, отличной от текущей. Применяется для построения alternate link.
        """
        request = RequestStub('ru')
        m_get_request.return_value = request
        url = 'rasp.yandex.ua/thread/123?foo=bar&big=bone&foo=baz'
        langified = langify(url, lang='ru', national_version='ua')

        assert langified == 'rasp.yandex.ua/thread/123?foo=bar&big=bone&foo=baz&lang=ru'
        m_get_language.assert_not_called()
        m_get_request.assert_not_called()
