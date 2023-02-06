# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

'''
Блокирует запросы по http.

Разблокировать можно двумя способами:

1. Зарегистрировав обработчик в httpretty через специальную фикстуру:

    def test_http_mock(httpretty):
        httpretty.register_uri(httpretty.GET, 'http://ya.ru')
        assert requests.get('http://ya.ru').status_code == 200

Или через модуль httpretty напрямую:

    @pytest.mark.usefixtures('httpretty')
    def test_http_mock():
        import httpretty
        httpretty.register_uri(httpretty.GET, 'http://ya.ru')
        assert requests.get('http://ya.ru').status_code == 200

2. Добавив пометку в тест:

    @pytest.mark.http_allowed
    def test_real_http_request():
        assert requests.get('http://localhost:8080').status_code == 200

'''

import re

import pytest
from httpretty import HTTPretty

ANY_PATTERN = re.compile(r'.*', re.M)


def _http_fail(_request, url, _headers):
    pytest.fail('Тест обращается к {}. Используйте фикстуру httpretty, чтобы исключить такие запросы.'.format(url))


def register_http_fail():
    for method in HTTPretty.METHODS:
        HTTPretty.register_uri(method, ANY_PATTERN, priority=-1, body=_http_fail)


@pytest.yield_fixture(scope='session', autouse=True)
def _httpretty_block():
    HTTPretty.enable()
    register_http_fail()
    try:
        yield
    finally:
        HTTPretty.reset()
        HTTPretty.disable()


@pytest.yield_fixture(scope='function')
def httpretty():
    assert HTTPretty.is_enabled()
    try:
        yield HTTPretty
    finally:
        HTTPretty.reset()
        register_http_fail()


def pytest_configure(config):
    config.addinivalue_line('markers', 'http_allowed: Разрешает запросы по http')


@pytest.mark.hookwrapper
def pytest_pyfunc_call(pyfuncitem):
    if not HTTPretty.is_enabled() or 'http_allowed' not in pyfuncitem.keywords:
        yield
        return

    HTTPretty.disable()
    try:
        yield
    finally:
        HTTPretty.enable()


@pytest.yield_fixture()
def http_allowed():
    HTTPretty.disable()
    try:
        yield
    finally:
        HTTPretty.enable()
