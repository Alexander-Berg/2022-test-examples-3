import pytest
from cryptography.fernet import InvalidToken

from sendr_utils.crypto.cryptogram import CryptogramCrypter, FernetBackend


class TestCrypter:
    @pytest.fixture
    def crypter(self):
        return CryptogramCrypter({
            1: FernetBackend({
                1: 'd4_Wjj6-xsld-QzCRfOguxSyVRP6MlP9XlcgazbjvMs=',
            }),
            2: FernetBackend({
                1: 'd4_Wjj6-xsld-QzCRfOguxSyVRP6MlP9XlcgazbjvMs=',
                2: 'B90ztqDUFOmr9QMk_peK9sLIncI03rI7_PLLiiUGnCg='
            }),
        })

    def test_encrypt(self, crypter):
        result = crypter.encrypt('babecafe')
        assert result.startswith('2.2.')
        assert crypter.decrypt(result) == 'babecafe'

    def test_decrypt(self, crypter):
        result = crypter.decrypt(
            '2.2.gAAAAABij6P9L7QlWb5iI8nZlEVYJb4tUYPOVmBfo0x4z-WoQ4JJ-N4F24Qh37scX4tz5-Sqb6G_SxSIvv1bCPwHcpV7U9Ov0A==',
        )
        assert result == 'cafebabe'

    def test_rotate(self, crypter):
        result = crypter.rotate(
            '1.1.gAAAAABij6Q4C-sVnW28UXyW1pnuJkDC1TyU0ntGZAmtnHBScSBldWgGZdC4ZVhOkcSyFWKs79AXFLDbdBrhd795h2bIl9VysQ==',
        )
        assert result.startswith('2.2')
        assert crypter.decrypt(result) == 'cafebabe'


class TestFernetBackend:
    @pytest.fixture
    def backend(self):
        return FernetBackend({
            1: 'd4_Wjj6-xsld-QzCRfOguxSyVRP6MlP9XlcgazbjvMs=',
            2: 'B90ztqDUFOmr9QMk_peK9sLIncI03rI7_PLLiiUGnCg='
        })

    def test_encrypt(self, backend):
        plaintext = b'ea495aad37cf4ee4a091958a02d1250288ee9e2b2e4249c3b76756f9102e4298'
        result = backend.encrypt(plaintext)

        assert isinstance(result, bytes)
        assert plaintext not in result
        assert backend.decrypt(result) == plaintext

    def test_decrypt(self, backend):
        result = backend.decrypt(
            b'2.gAAAAABiMGV2KS_4OfbAsgntH85z8wLH5kQulb6TY6SSp6WTGzSVdjSl9CY4xK11Lq8F78gMWPQBe6Vxe5DGy41716_0WgNvxw==',
        )
        assert result == b'babecafe'

    def test_decrypt_old(self, backend):
        result = backend.decrypt(
            b'1.gAAAAABiMGXssXchBYUUTkHGOJQT50UKGJ68rCZJIgwRcgHmHR_Y8-cvvT3ZXrub98HXo5EENzRgFzaPXyae1KduxT3sVS4f0g==',
        )
        assert result == b'cafebabe'

    def test_raises_on_empty_keyset(self):
        with pytest.raises(AssertionError):
            FernetBackend({})

    def test_unknown_kid(self, backend):
        token = (
            b'1337.gAAAAABiMGV2KS_4OfbAsgntH85z8wLH5kQulb6TY6SSp6WTGzSVdjSl9CY4xK11Lq8F78gMWPQBe6Vxe5DGy41716_0WgNvxw=='
        )
        with pytest.raises(InvalidToken):
            backend.decrypt(token)
