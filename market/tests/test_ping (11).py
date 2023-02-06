# coding: utf-8

import pytest

from market.idx.admin.dukalis.lib.app import create_flask_app
from market.idx.admin.dukalis.lib.settings import Settings


@pytest.fixture(scope='module')
def test_app():
    settings = Settings(statefile='./state.json')
    return create_flask_app(settings)


def test_ping(test_app):
    with test_app.test_client() as client:
        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.data == b'0;ok'


def test_close(test_app):
    with test_app.test_client() as client:
        resp = client.get('/close')
        assert resp.status_code == 200
        assert resp.data == b'closed'

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data == b'closed'

        resp = client.get('/open')
        assert resp.status_code == 200
        assert resp.data == b'0;ok'

        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.data == b'0;ok'
