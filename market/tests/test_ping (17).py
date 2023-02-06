# coding: utf-8
import os

import pytest

from lib.app import create_flask_app
from lib.settings import Settings


@pytest.fixture
def mock_redis(mocker):
    info = mocker.Mock(**{'master_for.return_value.info.return_value': '{}'})
    mocker.patch('lib.app.Sentinel', return_value=info)


@pytest.fixture()
def app(mock_redis):
    os.environ["CH_CACHE_SECRET"] = '{"redis_password":"test password"}'
    settings = Settings(statefile='./state.json')
    return create_flask_app(settings)


def test_ping(app):
    with app.test_client() as client:
        resp = client.get('/ping', )
        assert resp.status_code == 200
        assert resp.data == b'0;ok'


def test_close(app):
    with app.test_client() as client:
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
