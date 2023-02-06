# -*- coding: utf-8 -*-

import copy
import json
import pytest
from mock import patch
from six.moves.urllib.parse import urlencode

from market.idx.api.backend.blueprints.override_config import (
    set_config_values,
    remove_config_options,
    ConfigValueFormatter,
)

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage


class KazooClientMock():
    def get(self, zk_path):
        return dict(), None

    def set(self, zk_path, value):
        pass

    def create(self, zk_path, makepath=True):
        pass

    def delete(self, zk_path):
        pass

    def stop(self):
        pass

    def close(self):
        pass


class JugglerEventSenderMock():
    def send_event(self, host, service, status, description, tags=None):
        pass


patch_zkclient = patch(
    'market.pylibrary.zkclient.Client',
    auto_spec=True,
    return_value=KazooClientMock()
)

patch_juggler_client = patch(
    'market.idx.api.backend.blueprints.override_config.JugglerEventSender',
    auto_spec=True,
    return_value=JugglerEventSenderMock()
)


@pytest.fixture(scope="module")
def test_app():
    return create_flask_app(Storage())


@pytest.fixture(scope='function')
def test_config():
    return {
        'section0': {
            'option0': 'value0',
            'option1': 'value1',
            'option2': 'value2'
        },

        'section1': {
            'option0': 'value0',
            'option1': 'value1',
            'option2': 'value2'
        },

        'section2': {
            'option0': 'value0',
            'option1': 'value1',
            'option2': 'value2'
        }
    }


def test_remove_config_options(test_config):
    options_to_remove = [
        ('section1', 'option0'),
        ('section2', 'option1')
    ]
    remove_config_options(test_config, options_to_remove)
    assert ('option0' not in test_config['section1']) and ('option1' not in test_config['section2'])


def test_remove_repeated_config_options(test_config):
    config_copy = copy.deepcopy(test_config)
    options_to_remove = [
        ('section1', 'option1'),
        ('section1', 'option1')
    ]
    with pytest.raises(Exception):
        remove_config_options(test_config, options_to_remove)
    assert test_config == config_copy


def test_remove_config_options_not_exist(test_config):
    config_copy = copy.deepcopy(test_config)
    options_to_remove = [
        ('section10', 'option5'),
        ('section1', 'option6')
    ]
    remove_config_options(test_config, options_to_remove)
    assert test_config == config_copy


def test_remove_empty_section(test_config):
    options_to_remove = [
        ('section1', 'option0'),
        ('section1', 'option1'),
        ('section1', 'option2')
    ]
    remove_config_options(test_config, options_to_remove)
    assert 'section1' not in test_config


def test_set_config_values(test_config):
    values_to_set = [
        ('section0', 'option3', 'value3'),
        ('section1', 'option3', 'value0')
    ]
    set_config_values(test_config, values_to_set)
    assert test_config['section0']['option3'] == 'value3' and test_config['section1']['option3'] == 'value0'


def test_set_config_values_exist(test_config):
    values_to_set = [
        ('section0', 'option0', 'value5')
    ]
    set_config_values(test_config, values_to_set)
    assert test_config['section0']['option0'] == 'value5'


def test_set_config_values_repeated(test_config):
    config_copy = copy.deepcopy(test_config)
    values_to_set = [
        ('section0', 'option1', 'value4'),
        ('section0', 'option1', 'value5')
    ]
    with pytest.raises(Exception):
        set_config_values(test_config, values_to_set)
    assert test_config == config_copy


def test_show_config_values(test_config):
    formatter = ConfigValueFormatter()
    assert test_config == json.loads(formatter.jsonify(test_config))


def test_response_get_config_values(test_app):
    params = {
        'zk-path': '/test',
    }
    url = '/v1/override_config/show?' + urlencode(params)
    with test_app.test_client() as client, patch_zkclient, patch_juggler_client:
        resp = client.get(url)
        assert resp.status_code == 200


@pytest.mark.parametrize(
    "user, reason",
    [
        ('u', 'r'),
        ('bzz13', 'MARKETINDEXER-0000'),
        ('bzz13 Михайлов Виктор', 'MARKETINDEXER-0000'),
        ('bzz13 Михайлов Виктор', 'MARKETINDEXER-0000 - причина'),
    ]
)
def test_response_delete_config_options(test_app, user, reason):
    params = {
        'zk-path': '/test',
        'user': user,
        'reason': reason,
        'section': 's',
        'option': 'o',
    }
    url = '/v1/override_config/remove?' + urlencode(params)
    with test_app.test_client() as client, patch_zkclient, patch_juggler_client:
        resp = client.delete(url)
        assert resp.status_code == 200


@pytest.mark.parametrize(
    "user, reason",
    [
        ('u', 'r'),
        ('bzz13', 'MARKETINDEXER-0000'),
        ('bzz13 Михайлов Виктор', 'MARKETINDEXER-0000'),
        ('bzz13 Михайлов Виктор', 'MARKETINDEXER-0000 - причина'),
    ]
)
def test_response_delete_all_values(test_app, user, reason):
    params = {
        'zk-path': '/test',
        'user': user,
        'reason': reason,
    }
    url = '/v1/override_config/remove?' + urlencode(params)
    with test_app.test_client() as client, patch_zkclient, patch_juggler_client:
        resp = client.delete(url)
        assert resp.status_code == 200


@pytest.mark.parametrize(
    "user, reason",
    [
        ('u', 'r'),
        ('bzz13', 'MARKETINDEXER-0000'),
        ('bzz13 Михайлов Виктор', 'MARKETINDEXER-0000'),
        ('bzz13 Михайлов Виктор', 'MARKETINDEXER-0000 - причина'),
    ]
)
def test_response_put_config_values(test_app, user, reason):
    params = {
        'zk-path': '/test',
        'user': user,
        'reason': reason,
        'section': 's',
        'option': 'o',
        'value': 'v',
    }
    url = '/v1/override_config/set?' + urlencode(params)
    with test_app.test_client() as client, patch_zkclient, patch_juggler_client:
        resp = client.post(url)
        assert resp.status_code == 200
