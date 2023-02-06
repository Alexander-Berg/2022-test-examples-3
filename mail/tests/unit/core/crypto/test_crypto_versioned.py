from base64 import b64encode

import pytest

from mail.ipa.ipa.core.crypto.base import BlockEncryptor
from mail.ipa.ipa.core.crypto.versioned import (
    VersionedKeyStorage, VersionedManagedBlockDecryptor, VersionedManagedBlockEncryptor
)


@pytest.fixture
def message():
    return b'1' * 15 * 7


@pytest.fixture
def storage():
    return VersionedKeyStorage({'1': 'MTExMTExMTExMTExMTExMQ==', '2': 'MjIyMjIyMjIyMjIyMjIyMg=='})


@pytest.fixture
def encryptor(storage):
    return VersionedManagedBlockEncryptor(storage)


@pytest.fixture
def decryptor(storage):
    return VersionedManagedBlockDecryptor(storage)


class TestEncrypt:
    @pytest.fixture
    def ct(self, encryptor, message):
        return encryptor.update(message) + encryptor.finalize()

    def test_version(self, ct):
        assert ct.split(b':')[0] == b'2'

    def test_iv(self, ct, encryptor):
        assert ct.split(b':')[1] == b64encode(encryptor._encryptor.iv)


class TestDecryptUsesExactVersion:
    @pytest.fixture
    def old_encryptor(self):
        return VersionedManagedBlockEncryptor(VersionedKeyStorage({'1': 'MTExMTExMTExMTExMTExMQ=='}))

    @pytest.fixture
    def old_ct(self, message, encryptor):
        return encryptor.update(message) + encryptor.finalize()

    def test_decrypts(self, old_ct, message, decryptor):
        result = decryptor.update(old_ct) + decryptor.finalize()
        assert result == message


def test_encrypt_decrypt_with_strange_iv(mocker, encryptor, decryptor, message):
    strange_iv = b'1:2:3:4:5:6:7:8:'
    mocker.patch.object(BlockEncryptor, '_generate_iv', mocker.Mock(return_value=strange_iv))

    enc_dec = decryptor.update(encryptor.update(message) + encryptor.finalize()) + decryptor.finalize()
    assert message == enc_dec
