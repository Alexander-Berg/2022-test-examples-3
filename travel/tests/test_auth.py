# -*- coding: utf-8 -*-
import json

import pytest

from django.utils.http import urlencode
from django.conf import settings


@pytest.yield_fixture(scope='session')
def app(request):
    from travel.avia.avia_api.avia.application import create_app

    flask_app = create_app()

    app = flask_app.test_client()

    ctx = flask_app.app_context()
    ctx.push()

    yield app

    ctx.pop()


@pytest.mark.debugon
def test_debug_forbidden(app):
    assert app.get('v1.0/hello/guest/').status_code == 200
    assert app.get('v1.0/hello/appuser/').status_code == 403


HAND_URLS = [
    'v1.0/info/por/?lang=ru',
    # Без обязательного параметра. Должна вернуть не ошибку а forbidden
    'v1.0/info/por/',
    '/v1.0/search/',
    '/v1.0/search/results/',
    '/v1.0/order/',
    '/v1.0/order/redirect/',
]


@pytest.mark.parametrize('hand_url', HAND_URLS)
def test_forbidden(app, hand_url):
    assert app.get(hand_url).status_code == 403


@pytest.fixture()
def hello_app(app):
    params = {
        'uuid': '1234567890abcdef1234567890abcdef',
        'push_token': 'PUSH_TOKEN.1',
        'app_key': settings.APIKEYS_DEBUG_YKEY,
        'timestamp': '2014-12-12 12:12:12',
        'national_version': 'ru',
        'known_languages': '["ru","en"]',
        'known_currencies': '["RUR","USD"]',
    }

    r = app.post(
        'v2.0/hello/?' + urlencode(params),
        environ_base={'REMOTE_ADDR': '127.0.0.1'}
    )

    assert r.status_code == 200

    return app


@pytest.mark.debugon
def test_debug_hello(hello_app):
    assert hello_app.get('v1.0/hello/appuser/').status_code == 200


def test_failure_info_spec(hello_app):
    r = hello_app.get('v1.0/info/por/')
    assert r.status_code == 400

    d = json.loads(r.data)
    assert d['status'] == 'fail'
    assert d['data']['description']['errors'][0]['error'][0] == 'lang'
