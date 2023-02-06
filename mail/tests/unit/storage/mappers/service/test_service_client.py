import pytest

from sendr_utils import alist

from hamcrest import assert_that, has_properties, is_not

from mail.payments.payments.core.entities.service import ServiceClient


class TestServiceClientMapper:
    @pytest.fixture
    def service_client_entity(self, unique_rand, randn, rands, service):
        return ServiceClient(
            service_id=service.service_id,
            tvm_id=unique_rand(randn, basket='tvm_id'),
            api_callback_url=rands(),
        )

    @pytest.fixture
    async def returned(self, storage, service_client_entity):
        return await storage.service_client.create(service_client_entity)

    @pytest.fixture
    async def created(self, storage, returned):
        return await storage.service_client.get(returned.service_client_id)

    @pytest.mark.asyncio
    async def test_returned(self, service_client_entity, returned):
        assert_that(returned, has_properties({
            'service_id': service_client_entity.service_id,
            'tvm_id': service_client_entity.tvm_id,
            'api_callback_url': service_client_entity.api_callback_url,
            'service_client_id': is_not(None),
            'created': is_not(None),
            'updated': is_not(None),
        }))

    @pytest.mark.asyncio
    async def test_created(self, returned, created):
        assert created == returned

    @pytest.mark.asyncio
    async def test_find(self, storage, created):
        assert await alist(storage.service_client.find()) == [created]

    @pytest.mark.asyncio
    async def test_find_with_service(self, storage, service, created):
        created.service = service
        assert await alist(storage.service_client.find(with_service=True)) == [created]
