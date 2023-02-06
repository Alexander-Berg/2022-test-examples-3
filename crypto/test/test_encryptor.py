import binascii

import pytest

from crypta.s2s.services.conversions_downloader.lib.encrypter import Encrypter


def test_get_key_and_iv():
    """
    https://a.yandex-team.ru/arc_vcs/direct/libs/utils/src/test/java/ru/yandex/direct/utils/crypt/EncrypterTest.java?rev=r9427393#L50
    """
    secret = binascii.unhexlify("3239616362393361663561373265333161326535356330643431393133623666")
    salt = binascii.unhexlify("9fb9f092879ed999")
    actual_key, actual_iv = Encrypter.get_key_and_iv(secret, salt)

    assert binascii.hexlify(actual_key) == b"ec60791ad3b4383fa1f81be7868d892b14b4bf4846d2c766a7a2c46dbd19a7b7"
    assert binascii.hexlify(actual_iv) == b"210b94f1746112b48928ecbea76840cc"


def test_get_key_and_iv__wrong_salt_len():
    secret = binascii.unhexlify("3239616362393361663561373265333161326535356330643431393133623666")
    salt = b""

    with pytest.raises(ValueError):
        Encrypter.get_key_and_iv(secret, salt)


def test_encrypt_bytes():
    """
    https://a.yandex-team.ru/arc_vcs/direct/libs/utils/src/test/java/ru/yandex/direct/utils/crypt/EncrypterTest.java?rev=r9427393#L43
    """
    encrypted = Encrypter.encrypt_bytes(b"12345678", b"29acb93af5a72e31a2e55c0d41913b6f", b"        ")
    assert binascii.hexlify(encrypted) == b"53616c7465645f5f202020202020202001334e8b440b3ebeba755dcbb46a96bd"


def test_encrypt_decrypt():
    encrypter = Encrypter(b"AgDi7hzOy2LAUpFk9wJy")
    text = "12345678"
    assert encrypter.decrypt(encrypter.encrypt(text)) == text


def test_decrypt__without_salted_prefix():
    encrypter = Encrypter(b"AgDi7hzOy2LAUpFk9wJy")
    encrypted = binascii.hexlify(b"xxx")
    with pytest.raises(ValueError):
        encrypter.decrypt(encrypted)
