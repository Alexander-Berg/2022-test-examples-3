# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

import mock
import pytest

from library.python.vault_client.errors import ClientError

from travel.library.python.rasp_vault import api
from travel.library.python.rasp_vault.api import SecretNotFoundError, YavSecretProvider, Secrets


@pytest.fixture(autouse=True)
def gen_secrets():
    secrets = api.Secrets(YavSecretProvider())

    with mock.patch.object(api, '_secrets', secrets):
        yield


def build_version(version, secret_uuid, value):
    return {
        'secret_uuid': secret_uuid,
        'value': value,
        'version': version
    }


class SecretSource(object):
    def __init__(self, uuid, alias, last_version, value, tags=None, roles=None):
        self.uuid = uuid
        self.alias = alias
        self.last_version = last_version
        self.value = value
        self.tags = tags or []
        self.roles = roles or []

    @property
    def get_secret_result(self):
        return {
            'uuid': self.uuid,
            'name': self.alias,
            'secret_versions': [{'version': self.last_version}],
            'tags': self.tags or [],
            'secret_roles': self.roles or []
        }

    @property
    def list_secret_result(self):
        return {
            'uuid': self.uuid,
            'name': self.alias,
            'last_secret_version': {'version': self.last_version},
            'tags': self.tags or [],
            'secret_roles': self.roles or []
        }

    @property
    def get_version_result(self):
        return {
            'secret_uuid': self.uuid,
            'value': self.value,
            'version': self.last_version
        }


def build_valut_mock(*secret_sources):
    secrets = [ss.get_secret_result for ss in secret_sources]
    versions = [ss.get_version_result for ss in secret_sources]
    list_secrets = [ss.list_secret_result for ss in secret_sources]

    client = mock.Mock()

    def get_secret_stub(secret_id, **kwargs):
        for s in secrets:
            if s['uuid'] == secret_id:
                return s
        else:
            raise ClientError()

    client.get_secret.side_effect = get_secret_stub

    def list_secrets_stub(**kwargs):
        page_size = kwargs.get('page_size', 50)
        page = kwargs.get('page', 0)

        return list_secrets[page * page_size:(page + 1) * page_size]

    client.list_secrets.side_effect = list_secrets_stub

    def get_version_stub(version_id, **kwargs):
        for v in versions:
            if v['version'] == version_id:
                return v
        else:
            raise ClientError()

    client.get_version.side_effect = get_version_stub
    return client


def test_full_secret_by_id():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets):
        assert api.get_secret('sec-01') == {'key': 'some_key'}

    client_mock.get_secret.assert_called_once_with('sec-01', page_size=1)
    client_mock.get_version.assert_called_once_with('ver-02')
    client_mock.list_secrets.assert_not_called()


def test_get_key_by_path_with_secret_id():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets):
        assert api.get_secret('sec-01.key') == 'some_key'

    client_mock.get_secret.assert_called_once_with('sec-01', page_size=1)
    client_mock.get_version.assert_called_once_with('ver-02')
    client_mock.list_secrets.assert_not_called()


def test_get_secret_by_name_not_sec():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets), \
            mock.patch.object(api, 'sleep'):
        assert api.get_secret('mysecret-name') == {'key': 'some_key'}

    client_mock.get_secret.assert_not_called()
    client_mock.get_version.assert_called_once_with('ver-02')
    client_mock.list_secrets.assert_has_calls([mock.call(page_size=50, page=0), mock.call(page_size=50, page=1)])


def test_get_secret_by_name_starting_with_sec():
    client_mock = build_valut_mock(SecretSource('sec-01', 'secret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets), \
            mock.patch.object(api, 'sleep'):
        assert api.get_secret('secret-name') == {'key': 'some_key'}

    client_mock.get_secret.assert_has_calls([mock.call('secret-name', page_size=1)] * 5)
    client_mock.get_version.assert_called_once_with('ver-02')
    client_mock.list_secrets.assert_has_calls([mock.call(page_size=50, page=0), mock.call(page_size=50, page=1)])


def test_get_key_by_path_with_secret_name():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets), \
            mock.patch.object(api, 'sleep'):
        assert api.get_secret('mysecret-name') == {'key': 'some_key'}

    client_mock.get_secret.assert_not_called()
    client_mock.get_version.assert_called_once_with('ver-02')
    client_mock.list_secrets.assert_has_calls([mock.call(page_size=50, page=0), mock.call(page_size=50, page=1)])


def test_get_non_existed_secret():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets), \
            mock.patch.object(api, 'sleep'):
        with pytest.raises(SecretNotFoundError):
            with mock.patch.object(os, 'environ', {}):
                api.get_secret('non-existed')

    client_mock.get_secret.assert_not_called()
    client_mock.get_version.assert_not_called()
    client_mock.list_secrets.assert_has_calls([mock.call(page_size=50, page=0), mock.call(page_size=50, page=1)])


def test_get_non_existed_secret_in_test():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets), \
            mock.patch.object(api, 'sleep'):
        assert api.get_secret('non-existed') is None

    client_mock.get_secret.assert_not_called()
    client_mock.get_version.assert_not_called()
    client_mock.list_secrets.assert_has_calls([mock.call(page_size=50, page=0), mock.call(page_size=50, page=1)])


def test_cache():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets), \
            mock.patch.object(api, 'sleep'):
        assert api.get_secret('mysecret-name') == {'key': 'some_key'}
        assert api.get_secret('mysecret-name') == {'key': 'some_key'}
        assert api.get_secret('sec-01') == {'key': 'some_key'}
        assert api.get_secret('sec-01') == {'key': 'some_key'}

    client_mock.get_secret.assert_called_once_with('sec-01', page_size=1)
    client_mock.get_version.assert_called_once_with('ver-02')
    client_mock.list_secrets.assert_has_calls([mock.call(page_size=50, page=0), mock.call(page_size=50, page=1)])


def test_ignore_errors_not_found():
    client_mock = build_valut_mock(SecretSource('sec-01', 'mysecret-name', 'ver-02', {'key': 'some_key'}))
    client_mock.get_secret.side_effect = ClientError
    secrets = Secrets(YavSecretProvider(client_mock))
    with mock.patch.object(api, '_secrets', secrets):
        try:
            os.environ['RASP_VAULT_STUB_SECRETS'] = '1'
            assert api.get_secret('p.p') is None
            assert api.get_secret('p.p', 20) == 20
            assert api.get_secret('p', {'a': 10}) == {'a': 10}
        finally:
            del os.environ['RASP_VAULT_STUB_SECRETS']
