# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import gzip
import json
import os

import mock
import pytest
import six

from travel.library.python.rasp_vault import api
from travel.library.python.rasp_vault.api import FileBasedSecretProvider


def _write(f, data):
    result = json.dumps(data)
    if six.PY3:
        result = bytes(result, 'utf-8')
    f.write(result)


@pytest.fixture(autouse=True)
def gen_secrets(tmpdir):
    f1 = tmpdir.join('m.json.gz')
    with gzip.open(str(f1), 'wb') as f:
        _write(f, {
            'secret-one': {
                'secret': 'sec-01cmayyy',
                'version': 'ver-01cmaxxx',
                'value': {
                    'password': 'some_password',
                }
            }
        })

    f2 = tmpdir.join('bb.json.gz')
    with gzip.open(str(f2), 'wb') as f:
        _write(f, {
            'secret-two': {
                'secret': 'sec-02cmayyy',
                'version': 'ver-02cmaxxx',
                'value': {
                    'key': 'some_key',
                }
            }
        })

    secrets = api.Secrets(FileBasedSecretProvider(str(tmpdir)))

    with mock.patch.object(api, '_secrets', secrets):
        yield


def test_default_secrets(tmpdir):
    f1 = tmpdir.join('m.json.gz')
    with gzip.open(str(f1), 'wb') as f:
        _write(f, {
            'ssm': {
                'secret': 'sec-01',
                'version': 'ver-01',
                'value': {
                    'poss': 'another_poss',
                }
            }
        })

    with mock.patch.dict(os.environ, {'RASP_VAULT_PATH': str(tmpdir)}):
        secrets = api.Secrets()
        assert secrets.get('ssm.poss') == 'another_poss'


def test_get_full_path():
    assert api.get_secret('secret-one.password') == 'some_password'


def test_alias_path():
    assert api.get_secret('secret-two') == {'key': 'some_key'}


def test_secret_id_path():
    assert api.get_secret('sec-01cmayyy.password') == 'some_password'


@pytest.mark.parametrize('secret_path', [
    'p',
    'p.p',
    'secret-one.no-key'
])
def test_not_found(secret_path):
    with pytest.raises(api.SecretNotFoundError):
        api.get_secret(secret_path)


@pytest.mark.parametrize('secret_path', [
    'p.p.p',
    10,
])
def test_secret_path_errors_found(secret_path):
    with pytest.raises(api.SecretFetchError):
        api.get_secret(secret_path)


def test_ignore_errors_not_found():
    try:
        os.environ['RASP_VAULT_STUB_SECRETS'] = '1'
        assert api.get_secret('p.p') is None
    finally:
        del os.environ['RASP_VAULT_STUB_SECRETS']


def build_valut_mock():
    client = mock.Mock()
    secret = {
        'uuid': 'sec-01',
        'name': 'mysecret-name',
        'last_secret_version': {'version': 'ver-02'}
    }
    client.get_secret.return_value = secret
    client.list_secrets.return_value = [
        [secret],
        []
    ]
    client.get_version = {
        'value': {
            'key': 'some_key'
        }
    }
    return client
