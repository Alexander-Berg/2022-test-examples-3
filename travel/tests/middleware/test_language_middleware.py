# coding: utf-8
import mock
from django.http import HttpResponseRedirect
from django.test.client import RequestFactory

from common.middleware import language as language_module
from common.tester.testcase import TestCase
from travel.rasp.library.python.common23.date import environment


@mock.patch.object(language_module, 'detect_lang')
@mock.patch.object(environment, 'get_request')
class TestLanguageMiddlewareWithoutLangParameter(TestCase):
    def test_ua_uk(self, m_get_request, m_detect_lang):
        request = RequestFactory().get('/some/')
        request.NATIONAL_VERSION = 'ua'
        request.uatraits_result = {'isRobot': False}
        m_detect_lang.return_value = 'uk'
        m_get_request.return_value = request

        result = language_module.LanguageMiddleware().process_request(request)

        assert result is None
        assert request.LANGUAGE_CODE == 'uk'

    def test_ua_ru(self, m_get_request, m_detect_lang):
        request = RequestFactory().get('/some/')
        request.NATIONAL_VERSION = 'ua'
        request.uatraits_result = {'isRobot': False}
        m_detect_lang.return_value = 'ru'
        m_get_request.return_value = request

        result = language_module.LanguageMiddleware().process_request(request)

        assert isinstance(result, HttpResponseRedirect)
        assert result.url == 'http://testserver/some/?lang=ru'

    def test_bot(self, m_get_request, m_detect_lang):
        request = RequestFactory().get('/some/')
        request.NATIONAL_VERSION = 'ua'
        request.uatraits_result = {'isRobot': True}

        result = language_module.LanguageMiddleware().process_request(request)

        assert result is None
        assert request.LANGUAGE_CODE == 'uk'
        m_get_request.assert_not_called()
        m_detect_lang.assert_not_called()
