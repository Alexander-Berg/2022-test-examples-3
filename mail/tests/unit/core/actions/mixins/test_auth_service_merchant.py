import pytest

from mail.payments.payments.core.actions.mixins.auth_service_merchant import AuthServiceMerchantMixin
from mail.payments.payments.core.exceptions import CoreActionDenyError
from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture
def params(service_client, service_merchant):
    return {'service_tvm_id': service_client.tvm_id, 'service_merchant_id': service_merchant.service_merchant_id}


@pytest.fixture
def action(params, storage):
    class FinalAction(AuthServiceMerchantMixin):
        def __init__(self, service_merchant_id=None, service_tvm_id=None, service=None, uid=None):
            super().__init__()
            self.service_merchant_id = service_merchant_id
            self.service_tvm_id = service_tvm_id
            self.service = service
            self.uid = uid
    with temp_setattr(FinalAction.context, 'storage', storage):
        yield FinalAction(**params)


class TestServiceNotFound:
    @pytest.fixture
    def params(self, service_client, service_merchant):
        return {
            'service_tvm_id': service_client.tvm_id * 100,
            'service_merchant_id': service_merchant.service_merchant_id,
        }

    @pytest.mark.asyncio
    async def test_service_not_found(self, action):
        with pytest.raises(CoreActionDenyError):
            await action.authorize_service_merchant()


class TestServiceMerchantNotFound:
    @pytest.fixture
    def params(self, service_client, service_merchant):
        return {
            'service_tvm_id': service_client.tvm_id,
            'service_merchant_id': service_merchant.service_merchant_id * 100,
        }

    @pytest.mark.asyncio
    async def test_service_not_found(self, action):
        with pytest.raises(CoreActionDenyError):
            await action.authorize_service_merchant()


class TestServiceMerchantNotEnabled:
    @pytest.fixture
    async def disabled_service_merchant(self, service_merchant, storage):
        service_merchant.enabled = False
        return await storage.service_merchant.save(service_merchant)

    @pytest.mark.asyncio
    async def test_service_not_found(self, action, disabled_service_merchant):
        with pytest.raises(CoreActionDenyError):
            await action.authorize_service_merchant()


class TestServiceMerchantAuthorized:
    @pytest.mark.asyncio
    async def test_service_merchant_authorized(self, action, service_with_related):
        await action.authorize_service_merchant()
        assert all((
            action.service == service_with_related,
            action.uid == service_with_related.service_merchant.uid,
            action.service_client_id == action.service.service_client.service_client_id
        ))
