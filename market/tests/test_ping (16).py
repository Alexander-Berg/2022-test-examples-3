# coding: utf-8

import pytest

from lib.app import create_flask_app
from lib.settings import Settings


@pytest.fixture(scope='module')
def test_app():
    settings = Settings(statefile='./state.json')
    return create_flask_app(settings)


def test_ping(test_app):
    with test_app.test_client() as client:
        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.data.decode('utf-8') == '0;ok'


def test_close(test_app):
    with test_app.test_client() as client:
        resp = client.get('/close')
        assert resp.status_code == 200
        assert resp.data.decode('utf-8') == 'closed'

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data.decode('utf-8') == 'closed'

        resp = client.get('/open')
        assert resp.status_code == 200
        assert resp.data.decode('utf-8') == '0;ok'

        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.data.decode('utf-8') == '0;ok'
