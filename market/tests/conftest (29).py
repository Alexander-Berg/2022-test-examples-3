# coding: utf8


import os
import pytest
from pymongo import MongoClient
import yatest
from market.sre.services.balancer_api.lib.app import getApp
from market.sre.tools.balancer_regenerate.lib.resolver import Resolver


DB_NAME = 'test'


class ResolverStub(Resolver):
    def get_hosts(self, service_def):
        return [{'name': 'testservice.market.yandex.net', 'port': 80}]


@pytest.fixture
def app_settings(mongodb_server):
    test_dir = yatest.common.output_path()
    values_nginx_dir = os.path.join(test_dir, 'values-enabled-demo')

    if not os.path.exists(values_nginx_dir):
        os.mkdir(values_nginx_dir)

    class BalancerMock(object):
        def regenerate(self):
            pass

        def reload(self):
            pass

    return {
        'BALANCER_CONDUCTOR_GROUP': 'market_slb-front-testing',
        'TEMPLATES_DIR': yatest.common.source_path("market/sre/services/balancer_api/etc/yandex/market-balancer-api/templates"),
        'VALUES_NGINX_DIR': values_nginx_dir,
        'LOG_PATH': '/var/log/yandex/market-balancer-api/access.log',
        'HOST': '::',
        'PORT': '4242',
        'LOG_LEVEL': 'DEBUG',
        'LOGGING_FORMAT': '%(asctime)s %(levelname)s: %(message)s',
        'STATE_FILE': '/var/tmp/market-balancer-api.state',
        'DB_NAME': DB_NAME,
        'DB_URI': mongodb_server.uri,
        'balancer': BalancerMock(),
        'SSL_CERT_PATH': '/usr/share/yandex-internal-root-ca/YandexInternalRootCA.crt',
        'DEBUG': True,
        'TESTING': True,
    }


@pytest.fixture
def app(app_settings):
    return getApp(app_settings)


@pytest.fixture
def client(app):
    client = app.test_client()
    yield client


@pytest.fixture(autouse=True)
def run_around_tests(mongodb_server):
    # Setup code before each test

    # Clear test database
    client = MongoClient(mongodb_server.uri)
    client[DB_NAME].config.delete_many({})
    client[DB_NAME].journal.drop()

    yield

    # Cleanup code after each test
