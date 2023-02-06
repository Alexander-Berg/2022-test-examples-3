# coding: utf8
import collections
import pytest
import six

from market.idx.datacamp.system_offers.lib.app import create_flask_app

Settings = collections.namedtuple('Settings', ['env', 'statefile'])


@pytest.fixture(scope='module')
def test_app():
    settings = Settings('testing', './state.json')
    return create_flask_app(settings, None)


def s(data):
    return six.ensure_str(data)


def test_ping(test_app):
    with test_app.test_client() as client:
        resp = client.get('/ping')
        assert resp.status_code == 200
        assert s(resp.data) == '0;OK'


def test_close(test_app):
    with test_app.test_client() as client:
        resp = client.get('/close')
        assert resp.status_code == 200
        assert s(resp.data) == '0;OK'

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert s(resp.data) == '2;CLOSED'

        resp = client.get('/open')
        assert resp.status_code == 200
        assert s(resp.data) == '0;OK'

        resp = client.get('/ping')
        assert resp.status_code == 200
        assert s(resp.data) == '0;OK'
