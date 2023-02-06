import pytest

from travel.avia.subscriptions.app.settings.secrets import SettingsSecretResolver


@pytest.fixture(scope='session')
def vault_client():
    class FakeVaultClient:
        def __init__(self, *args, **kwargs):
            pass

        def get_version(self, key):
            secrets = {
                'sec-123': {
                    'password': '456',
                },
                'ver-abc': {
                    'token': 'def',
                    'proxy': 'ghi',
                },
                'sec-bcd': {},
            }
            return dict(value=secrets.get(key))

    return FakeVaultClient


def test_settings_secret_resolver_suit(vault_client):
    fs = dict(a='sec-123', b='sec-123.password', c=dict(d='ver-abc.token', e='4'), f=4.56)
    ssr = SettingsSecretResolver('yav-token', vault_client)

    d = ssr.resolve(fs)
    assert d == dict(a='456', b='456', c=dict(d='def', e='4'), f=4.56)


def test_settings_secret_resolver_fails_on_empty_secret(vault_client):
    fs = dict(a='sec-123', b='sec-123.password', c=dict(d='ver-abc.token', e='sec-bcd'), f=4.56)
    ssr = SettingsSecretResolver('yav-token', vault_client)

    with pytest.raises(KeyError):
        ssr.resolve(fs)


def test_settings_secret_resolver_fails_on_ambiguous_secret(vault_client):
    fs = dict(a='sec-123', b='sec-123.password', c=dict(d='ver-abc', e='4'), f=4.56)
    ssr = SettingsSecretResolver('yav-token', vault_client)

    with pytest.raises(KeyError):
        ssr.resolve(fs)
