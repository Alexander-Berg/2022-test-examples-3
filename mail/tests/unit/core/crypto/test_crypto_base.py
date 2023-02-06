import os

import pytest

from mail.ipa.ipa.core.crypto.base import BlockDecryptor, BlockEncryptor


@pytest.fixture
def key():
    return os.urandom(32)


@pytest.fixture
def iv():
    return os.urandom(16)


@pytest.fixture
def encryptor(key, iv):
    return BlockEncryptor(key, iv)


@pytest.fixture
def decryptor(key, iv):
    return BlockDecryptor(key, iv)


def test_encryption(encryptor, decryptor):
    source_data = b'encryptplz'

    ct = encryptor.update(source_data) + encryptor.finalize()
    assert decryptor.update(ct) + decryptor.finalize() == source_data


class TestGeneratesIVWhenNoSuchSupplied:
    @pytest.fixture
    def encryptor(self, key):
        return BlockEncryptor(key)

    def test_generate_iv(self, encryptor):
        assert len(encryptor.iv) == encryptor.BLOCK_SIZE_IN_BYTES
