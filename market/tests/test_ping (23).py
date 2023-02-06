# coding: utf-8

import os
import pytest

from lib.app import create_flask_app
from lib.settings import Settings
import lib.state


@pytest.fixture(scope='module')
def test_app():
    with open('./cfg.pb.txt', "w") as config:
        config.write("""
BlueOffersCountConfig {
    ReportHost: "report.tst.vs.market.yandex.net"
    GraphiteHost: "blue-report-tst"
    SendPeriod: 300
    Dummy: True
    ReportPort: 17051
    MetricConfig: "test_cfg"
}""")
    os.environ['BSCONFIG_ITAGS'] = 'a_dc_sas a_ctype_prestable'
    settings = Settings(statefile='./state.json', config='./cfg.pb.txt')
    return create_flask_app(settings)


def reset_globals():
    lib.state.sender_threads = []
    lib.state.stop_event = None
    lib.state.configs = []


def test_ping(test_app):
    reset_globals()
    with test_app.test_client() as client:
        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.data == '0;ok'


def test_close(test_app):
    reset_globals()
    with test_app.test_client() as client:
        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.data == '0;ok'

        resp = client.get('/close')
        assert resp.status_code == 200
        assert resp.data == 'closed'

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data == 'closed'

        resp = client.get('/open')
        assert resp.status_code == 200
        assert resp.data == '0;ok'

        resp = client.get('/ping')
        assert resp.status_code == 200
        assert resp.data == '0;ok'


def test_no_config():
    reset_globals()
    os.environ['BSCONFIG_ITAGS'] = 'a_dc_sas a_ctype_testing'
    settings = Settings(statefile='./state_.json')
    app = create_flask_app(settings)
    with app.test_client() as client:
        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data == "Service started without config!"

        resp = client.get('/close')
        assert resp.status_code == 200
        assert resp.data == 'closed'

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data == 'closed'

        resp = client.get('/open')
        assert resp.status_code == 200
        assert resp.data == "Service started without config!"

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data == "Service started without config!"


def test_bad_config():
    reset_globals()
    with open('./cfg_.pb.txt', "w") as config:
        config.write("""
BlueOffersCountConfig {
    aslkjdflaksjhdf: "report.tst.vs.market.yandex.net"
    GraphiteHost: "blue-report-tst"
    SendPeriod: 300
    Dummy: True
    ReportPort: 17051
    MetricConfig: "test_cfg"
}""")
    os.environ['BSCONFIG_ITAGS'] = 'a_dc_sas a_ctype_testing'
    settings = Settings(statefile='./state__.json', config='./cfg_.pb.txt')
    app = create_flask_app(settings)
    with app.test_client() as client:
        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data.find("Bad config file: ") != -1

        resp = client.get('/close')
        assert resp.status_code == 200
        assert resp.data == 'closed'

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data == 'closed'

        resp = client.get('/open')
        assert resp.status_code == 200
        assert resp.data.find("Bad config file: ") != -1

        resp = client.get('/ping')
        assert resp.status_code == 500
        assert resp.data.find("Bad config file: ") != -1
