# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.test import override_settings
from django.test.client import RequestFactory

from common.utils import request_helper


@pytest.mark.parametrize('env, host', [
    ({'HTTP_HOST': 'orig.ru'}, 'orig.ru'),
    ({'HTTP_X_REAL_HOST': 'real.ru', 'HTTP_HOST': 'orig.ru'}, 'real.ru'),
    ({'HTTP_X_REAL_HOST': 'real.ru', 'HTTP_X_FORWARDED_HOST': 'forw.ru', 'HTTP_HOST': 'orig.ru'}, 'real.ru'),
    ({'HTTP_X_FORWARDED_HOST': 'forw.ru', 'HTTP_HOST': 'orig.ru'}, 'forw.ru'),
])
def test_get_host(env, host):
    run_test_get_host(env, host)


@override_settings(ALLOWED_HOSTS=['orig.ru', 'real.ru', 'forw.ru'])
def run_test_get_host(env, host):
    rq = RequestFactory(**env).get('/here')
    assert request_helper.get_host(rq) == host


@pytest.mark.parametrize('env, host', [
    ({'HTTP_HOST': 'orig.ru'}, 'orig.ru'),
    ({'HTTP_X_REAL_HOST': 'real.ru', 'HTTP_HOST': 'orig.ru'}, 'real.ru'),
    ({'HTTP_X_REAL_HOST': 'real.ru', 'HTTP_X_FORWARDED_HOST': 'forw.ru', 'HTTP_HOST': 'orig.ru'}, 'real.ru'),
    ({'HTTP_X_FORWARDED_HOST': 'forw.ru', 'HTTP_HOST': 'orig.ru'}, 'forw.ru'),
])
def test_build_uri(env, host):
    run_test_get_host(env, host)


@override_settings(ALLOWED_HOSTS=['orig.ru', 'real.ru', 'forw.ru'])
def run_test_build_uri(env, host):
    rq = RequestFactory(**env).get('/here')
    assert request_helper.build_absolute_uri(rq) == 'http://{}/here'.format(host)

    rq = RequestFactory(**env).get('/here')
    assert request_helper.build_absolute_uri(rq, '/here-not') == 'http://{}/here-not'.format(host)
