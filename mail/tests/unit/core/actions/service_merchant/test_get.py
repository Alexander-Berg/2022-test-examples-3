import pytest

from mail.payments.payments.core.actions.service_merchant.get import (
    GetServiceMerchantAction, GetServiceMerchantListAction, GetServiceMerchantServiceAction
)
from mail.payments.payments.core.exceptions import CoreNotFoundError


class TestGetServiceMerchant:
    @pytest.fixture
    def params(self, service_client, service_merchant):
        return {'service_merchant_id': service_merchant.service_merchant_id,
                'service_tvm_id': service_client.tvm_id}

    @pytest.fixture
    def action(self):
        return GetServiceMerchantServiceAction

    @pytest.fixture
    async def returned(self, action, params):
        return await action(**params).run()

    def test_returns_service_merchant(self, service_merchant, returned, service):
        assert returned == service_merchant and returned.service == service


class TestGetServiceMerchantByUID(TestGetServiceMerchant):
    @pytest.fixture
    async def action(self, params):
        return GetServiceMerchantAction

    @pytest.fixture
    def params(self, service_merchant):
        return {'service_merchant_id': service_merchant.service_merchant_id,
                'uid': service_merchant.uid}


class TestGetNotExistingServiceMerchant:
    @pytest.fixture
    def params(self, randn, service_client):
        return {'service_merchant_id': randn(),
                'service_tvm_id': service_client.tvm_id}

    @pytest.fixture
    def action(self):
        return GetServiceMerchantServiceAction

    @pytest.mark.asyncio
    async def test_returns_not_found(self, action, params):
        with pytest.raises(CoreNotFoundError):
            await action(**params).run()


class TestServiceNotBelongToUID(TestGetNotExistingServiceMerchant):
    @pytest.fixture
    async def action(self):
        return GetServiceMerchantAction

    @pytest.fixture
    def params(self, service_merchant):
        return {'service_merchant_id': service_merchant.service_merchant_id,
                'uid': service_merchant.uid * 10}


class TestGetServiceMerchantNotBelongToService:
    @pytest.fixture
    def params(self, service_merchant, service_client):
        return {
            'service_tvm_id': service_client.tvm_id + 1,
            'service_merchant_id': service_merchant.service_merchant_id,
        }

    @pytest.mark.asyncio
    async def test_returns_not_found(self, params):
        with pytest.raises(CoreNotFoundError):
            await GetServiceMerchantServiceAction(**params).run()


class TestGetServiceMerchantList:
    @pytest.fixture
    def params(self, service_merchant):
        return {'uid': service_merchant.uid}

    @pytest.fixture
    async def returned(self, params):
        return await GetServiceMerchantListAction(**params).run()

    def test_returns_merchant(self, service_merchant, returned):
        assert returned == [service_merchant]
