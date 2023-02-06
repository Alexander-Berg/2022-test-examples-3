import pytest

from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
from mail.payments.payments.core.entities.enums import AcquirerType


class TestGetAcquirerMerchantAction:
    @pytest.fixture
    async def returned(self, merchant):
        return await GetAcquirerMerchantAction(uid=merchant.uid).run()

    def test_returns_acquirer(self, merchant, returned):
        assert returned == merchant.acquirer

    class TestWithParent:
        @pytest.fixture
        def parent_uid(self, parent_merchant):
            return parent_merchant.uid

        @pytest.fixture(autouse=True)
        async def setup_merchant(self, storage, merchant, parent_merchant):
            # Making sure child acquirer is different. This never happens in real conditions.
            merchant.acquirer = next((acquirer for acquirer in AcquirerType if acquirer != parent_merchant.acquirer))
            await storage.merchant.save(merchant)

        def test_returns_parent_acquirer(self, merchant, parent_merchant, returned):
            assert merchant.acquirer != returned == parent_merchant.acquirer
