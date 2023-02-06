from tractor_api.methods.external_secret import _set_external_secret, _get_external_secret
from tractor.models import ExternalProvider
from tractor.tests.fixtures.common import *
from unittest.mock import Mock, MagicMock, ANY
from pytest import fixture
from typing import Optional


class CiphertextMatcher:
    def __init__(self, fernet, plaintext: str):
        self.fernet = fernet
        self.plaintext = plaintext

    def __eq__(self, ciphertext: bytes):
        return self.fernet.decrypt_text(ciphertext) == self.plaintext


@fixture
def db_mock():
    return MagicMock()


def test_set_external_secret(db_mock: MagicMock):
    _set_external_secret(ORG_ID, DOMAIN, ExternalProvider.GOOGLE, EXTERNAL_SECRET, db_mock, FERNET)
    db_mock.set_external_secret.assert_called_with(
        ORG_ID,
        DOMAIN,
        ExternalProvider.GOOGLE,
        CiphertextMatcher(FERNET, EXTERNAL_SECRET),
        ANY,
    )


def test_get_external_secret_present(db_mock: MagicMock):
    db_mock.get_external_secret = Mock(return_value=FERNET.encrypt_text(EXTERNAL_SECRET))
    secret: Optional[str] = _get_external_secret(
        ORG_ID, DOMAIN, ExternalProvider.GOOGLE, db_mock, FERNET
    )
    db_mock.get_external_secret.assert_called_once_with(
        ORG_ID, DOMAIN, ExternalProvider.GOOGLE, cur=ANY
    )
    assert secret == EXTERNAL_SECRET


def test_get_external_secret_absent(db_mock: MagicMock):
    db_mock.get_external_secret = Mock(return_value=None)
    secret: Optional[str] = _get_external_secret(
        ORG_ID, DOMAIN, ExternalProvider.GOOGLE, db_mock, FERNET
    )
    db_mock.get_external_secret.assert_called_once_with(
        ORG_ID, DOMAIN, ExternalProvider.GOOGLE, cur=ANY
    )
    assert secret is None
