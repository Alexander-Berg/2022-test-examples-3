# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import pytest
import flask
from search.martylib.flask_utils.middleware.allowed_hosts import AllowedHostsMiddleware
from search.martylib.test_utils import PytestCase


class TestAllowedHostsMiddleware(PytestCase):
    @pytest.mark.parametrize('host', (
        'localhost',
        'localhost:8888',
        'martylib.search.yandex.net',
        'martylib.search.yandex.net:8888',
        'martylib.n.yandex-team.ru',
        'martylib.n.yandex-team.ru:8888',
    ))
    def test_wildcard(self, host):
        app = flask.Flask(__name__)
        middleware = AllowedHostsMiddleware(False, '*')
        with app.test_request_context('http://{}/'.format(host)):
            assert not middleware.process_request('GET', [], {}), flask.request.host

    @pytest.mark.parametrize('allowed_host,host,is_allowed', (
        ('.search.yandex.net', 'martylib.search.yandex.net', True),
        ('.search.yandex.net', 'martylib.search.yandex.ru', False),
        ('.search.yandex.net', 'martylib.search.yandex.net:8888', False),
    ))
    def test_subdomain_wildcard(self, allowed_host, host, is_allowed):
        app = flask.Flask(__name__)
        middleware = AllowedHostsMiddleware(False, allowed_host)
        with app.test_request_context('http://{}/'.format(host)):
            result = middleware.process_request('GET', [], {})
            assert not result if is_allowed else result, flask.request.host
