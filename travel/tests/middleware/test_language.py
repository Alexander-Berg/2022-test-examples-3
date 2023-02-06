# coding: utf-8

import pytest
from django.conf import settings
from django.test.client import RequestFactory

from common.middleware.language import LanguageMiddleware
from common.middleware.yauser import AnonymousYandexUser


@pytest.mark.parametrize('lang, national_version, expected', [
    ('uk', 'ua', 'uk'),
    ('ru', 'ua', 'ru'),
    ('unexpected_lang', 'ua', 'uk'),

    ('ru', 'ru', 'ru'),
    ('unexpected_lang', 'ru', 'ru'),

    ('ru', 'unexpected_tld', 'ru'),
    ('unexpected_lang', 'unexpected_tld', 'ru'),
])
def test_language_middleware(httpretty, lang, national_version, expected):
    httpretty.register_uri(httpretty.POST, settings.LANGDETECTSERVER_URL, status=404)
    request = RequestFactory().get('/some/?lang={}'.format(lang))
    request.NATIONAL_VERSION = national_version
    request.yauser = AnonymousYandexUser()
    request.host = 'rasp.yandex.{}'.format(national_version)
    request.uatraits_result = {'isRobot': False}
    result = LanguageMiddleware().process_request(request)
    assert result is None
    assert request.LANGUAGE_CODE == expected
