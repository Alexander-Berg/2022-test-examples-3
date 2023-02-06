# coding: utf-8

import collections
import mock
import pytest
import six

from market.idx.admin.system_offers.lib.app import create_flask_app
from market.idx.admin.system_offers.lib.config import make_object


Settings = collections.namedtuple('Settings', ['env', 'statefile'])


@pytest.fixture(scope='module')
def config():
    return make_object({
        'report': {
            'host': 'report_host',
            'port': 'report_port'
        },
        'blue_report': {
            'host': 'blue_report_host',
            'port': 'blue_report_port'
        },
        'saashub': {
            'host': 'saashub_host',
        }
    })


@pytest.fixture(scope='module')
def test_app(config):
    with mock.patch('market.idx.admin.system_offers.lib.blueprints.stalker.Blueprint'):
        settings = Settings('production', './state.json')
        return create_flask_app(settings, config)


def s(data):
    return six.ensure_str(data)


def test_offers(config, test_app):
    with test_app.test_client() as client:
        resp = client.get('/offers')
        assert resp.status_code == 200
        assert s(resp.data).find('{}'.format(config.saashub.host)) != -1
        assert s(resp.data).find('{}:{}'.format(config.report.host, config.report.port)) != -1
        assert s(resp.data).find('{}:{}'.format(config.blue_report.host, config.blue_report.port)) != -1
