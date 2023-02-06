import pytest

from hamcrest import assert_that, greater_than, has_properties

from mail.payments.payments.core.actions.service_merchant.delete import (
    DeleteServiceMerchantAction, DeleteServiceMerchantServiceAction
)
from mail.payments.payments.core.exceptions import ServiceMerchantNotFoundError


class TestDeleteServiceMerchant:
    @pytest.fixture
    def params(self, service_merchant):
        return {'service_merchant_id': service_merchant.service_merchant_id,
                'uid': service_merchant.uid}

    @pytest.fixture
    async def returned(self, params):
        return await DeleteServiceMerchantAction(**params).run()

    def test_returns_service_merchant_deleted(self, service_merchant, returned):
        assert_that(
            returned,
            has_properties({
                'service_merchant_id': service_merchant.service_merchant_id,
                'uid': service_merchant.uid,
                'service_id': service_merchant.service_id,
                'entity_id': service_merchant.entity_id,
                'description': service_merchant.description,
                'enabled': service_merchant.enabled,
                'created': service_merchant.created,
                'deleted': True,
                'updated': greater_than(service_merchant.updated),
                'revision': greater_than(service_merchant.revision),
            })
        )


class TestDeleteServiceMerchantNotBelongToUid:
    @pytest.fixture
    def params(self, service_merchant):
        return {'service_merchant_id': service_merchant.service_merchant_id,
                'uid': service_merchant.uid * 10}

    @pytest.mark.asyncio
    async def test_returns_not_found(self, params):
        with pytest.raises(ServiceMerchantNotFoundError):
            await DeleteServiceMerchantAction(**params).run()


class TestDeleteServiceMerchantServiceAction:
    @pytest.fixture
    def params(self, service_merchant, service_client):
        return {'service_merchant_id': service_merchant.service_merchant_id,
                'service_tvm_id': service_client.tvm_id}

    @pytest.fixture
    async def returned(self, params):
        return await DeleteServiceMerchantServiceAction(**params).run()

    def test_returns_service_merchant_deleted(self, service_merchant, returned):
        assert_that(
            returned,
            has_properties({
                'service_merchant_id': service_merchant.service_merchant_id,
                'uid': service_merchant.uid,
                'service_id': service_merchant.service_id,
                'entity_id': service_merchant.entity_id,
                'description': service_merchant.description,
                'enabled': service_merchant.enabled,
                'created': service_merchant.created,
                'deleted': True,
                'updated': greater_than(service_merchant.updated),
                'revision': greater_than(service_merchant.revision),
            })
        )

    @pytest.mark.asyncio
    async def test_service_merchant_marked_deleted_in_db(self, storage, service_merchant, returned):
        deleted_service_merchant = await storage.service_merchant.get(returned.service_merchant_id,
                                                                      ignore_deleted=False)
        assert deleted_service_merchant.deleted is True
