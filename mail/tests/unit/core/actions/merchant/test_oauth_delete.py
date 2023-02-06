import pytest

from mail.payments.payments.core.actions.merchant.oauth_delete import OAuthDeleteMerchantAction
from mail.payments.payments.core.entities.enums import MerchantOAuthMode


class TestOAuthDeleteMerchantAction:
    @pytest.fixture
    def action(self, merchant):
        return OAuthDeleteMerchantAction(uid=merchant.uid, merchant_oauth_mode=MerchantOAuthMode.PROD)

    @pytest.mark.asyncio
    async def test_returned(self, action):
        returned = await action.run()
        assert returned is None
