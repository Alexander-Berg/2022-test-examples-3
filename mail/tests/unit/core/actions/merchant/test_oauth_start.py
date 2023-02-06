from urllib.parse import urlencode

import pytest

from mail.payments.payments.core.actions.merchant.oauth_start import OAuthStartMerchantAction
from mail.payments.payments.core.exceptions import ChildMerchantError, MerchantNotFoundError
from mail.payments.payments.tests.base import BaseTestParent
from mail.payments.payments.utils.helpers import without_none


class TestOAuthStartMerchantAction(BaseTestParent):
    @pytest.fixture(autouse=True)
    def setup(self, mocker):
        mocker.patch(
            'mail.payments.payments.core.actions.merchant.oauth_start.uuid_hex',
            mocker.Mock(return_value='mocked')
        )

    @pytest.fixture
    def uid(self, merchant):
        return merchant.uid

    @pytest.fixture(params=(True, False))
    def state(self, request, rands):
        return rands() if request.param else None

    @pytest.fixture
    def params(self, state, uid):
        return {'state': state, 'uid': uid}

    @pytest.fixture
    def with_parent(self):
        return False

    @pytest.fixture
    def action(self, params):
        return OAuthStartMerchantAction(**params)

    @pytest.fixture
    async def returned(self, action):
        return await action.run()

    @pytest.mark.parametrize('uid', (-1,))
    @pytest.mark.asyncio
    async def test_merchant_not_found(self, action):
        with pytest.raises(MerchantNotFoundError):
            await action.run()

    @pytest.mark.parametrize('merchant_uid', ('123456', '12345'))
    @pytest.mark.asyncio
    async def test_url(self, returned, payments_settings, uid, state):
        params = {
            'response_type': 'code',
            'client_id': payments_settings.OAUTH_APP_ID,
            'device_id': 'payments-mocked',
            'state': state
        }
        url = f"{payments_settings.OAUTH_URL.rstrip('/')}/authorize?" + urlencode(without_none(params))

        assert url == returned

    class TestParent:
        @pytest.fixture
        def with_parent(self):
            return True

        @pytest.mark.asyncio
        async def test_parent(self, action):
            with pytest.raises(ChildMerchantError):
                await action.run()
