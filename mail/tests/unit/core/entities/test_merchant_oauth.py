from datetime import timedelta

import pytest

from sendr_utils import utcnow


class TestMerchantOAuth:
    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.core.entities.merchant_oauth.utcnow', mocker.Mock(return_value=now))
        return now

    def test_expires_in(self, merchant_oauth, now):
        assert merchant_oauth.expires_in == (merchant_oauth.expires - now).total_seconds()

    @pytest.mark.parametrize('field', ('access_token', 'refresh_token'))
    def test_encrypt(self, field, rands, merchant_oauth):
        value = rands()
        setattr(merchant_oauth, f'decrypted_{field}', value)
        assert value == getattr(merchant_oauth, f'decrypted_{field}')

    @pytest.mark.parametrize('now_delta,expired', (
        (timedelta(days=0), True),
        (timedelta(days=-1), True),
        (timedelta(days=1), False),
    ))
    def test_expired(self, merchant_oauth, now, now_delta, expired):
        merchant_oauth.expires = now + now_delta
        assert merchant_oauth.expired == expired
