import pytest

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


class TestGetMerchantToken(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def response_func(self, client, merchant, tvm):
        async def _inner(status=200):
            r = await client.get(f'/v1/merchant/{merchant.uid}/token')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_success(self, response_func, merchant):
        response = await response_func()
        assert response['data']['token'] == merchant.token


class TestPostMerchantToken(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def response_func(self, client, merchant, tvm):
        async def _inner(status=200):
            r = await client.post(f'/v1/merchant/{merchant.uid}/token')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_success(self, response_func, merchant, storage):
        response = await response_func()
        merchant = await storage.merchant.get(uid=merchant.uid)
        assert response['data']['token'] == merchant.token
