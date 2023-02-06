import pytest

from mail.payments.payments.core.actions.merchant.token import GetMerchantTokenAction, RegenerateMerchantTokenAction


@pytest.fixture(params=(False, True))
def with_parent(request):
    return request.param


@pytest.fixture
def parent_uid(with_parent, parent_merchant):
    return parent_merchant.uid if with_parent else None


class TestGetTokenMerchant:
    @pytest.fixture
    def params(self, merchant):
        return {'uid': merchant.uid}

    @pytest.fixture
    async def returned(self, params):
        return await GetMerchantTokenAction(**params).run()

    def test_returns_merchant_token(self, merchant, returned):
        assert returned == {'token': merchant.token}

    @pytest.mark.asyncio
    async def test_generate_merchant_token(self, merchant, params, storage):
        merchant.token = None
        await storage.merchant.save(merchant)
        returned = await GetMerchantTokenAction(**params).run()
        merchant = await storage.merchant.get(uid=merchant.uid)
        assert merchant.token is not None and returned == {'token': merchant.token}


class TestRegenerateTokenMerchant:
    @pytest.fixture
    def params(self, merchant):
        return {'uid': merchant.uid}

    @pytest.mark.asyncio
    async def test_returns_updated_merchant_token(self, merchant, params, storage):
        old_token = merchant.token
        returned = await RegenerateMerchantTokenAction(**params).run()
        merchant = await storage.merchant.get(uid=merchant.uid)
        assert returned['token'] != old_token and returned == {'token': merchant.token}
