# coding: utf-8

import pytest
from fastapi.testclient import TestClient

from lib.app import app


@pytest.fixture(scope='module')
def test_app():
    return TestClient(app)


def test_ping(test_app):
    with test_app as client:
        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.text == '0;ok'


def test_monitoring(test_app):
    with test_app as client:
        resp = client.get('/monitoring')
        assert resp.status_code == 200
        assert resp.text == '0;ok'


def test_close(test_app):
    with test_app as client:
        resp = client.get('/close')
        assert resp.status_code == 200
        assert resp.text == 'closed'

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.text == 'closed'

        resp = client.get('/open')
        assert resp.status_code == 200
        assert resp.text == '0;ok'

        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.text == '0;ok'
