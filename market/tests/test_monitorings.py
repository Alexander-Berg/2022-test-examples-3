import pytest
from fastapi.testclient import TestClient

from market.robotics.storage_service.lib.app import app


@pytest.fixture(scope='module')
def test_app():
    return TestClient(app)


def test_ping(test_app):
    with test_app as client:
        resp = client.get('monitoring/ping')
        assert resp.status_code == 200
        assert resp.text == '0;ok'


def test_status(test_app):
    with test_app as client:
        resp = client.get('monitoring/status')
        assert resp.status_code == 200
        data = resp.json()
        assert set(data.keys()) == set(["opened", "revision"])
        assert data["opened"]
