import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.service_client import CreateServiceClientAction
from mail.payments.payments.core.exceptions import ServiceNotFoundError


class TestCreateServiceClientAction:
    @pytest.fixture
    def tvm_id(self, unique_rand, randn):
        return unique_rand(randn, basket='tvm_id')

    @pytest.fixture
    def api_callback_url(self):
        return 'test-create-service-client-action-api_callback_url'

    @pytest.fixture
    def params(self, service, tvm_id, api_callback_url):
        return {
            'service_id': service.service_id,
            'tvm_id': tvm_id,
            'api_callback_url': api_callback_url,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await CreateServiceClientAction(**params).run()
        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def created_service_client(self, storage, returned):
        return await storage.service_client.get(returned.service_client_id)

    def test_returned(self, service, tvm_id, api_callback_url, returned):
        assert_that(
            returned,
            has_properties({
                'service_id': service.service_id,
                'tvm_id': tvm_id,
                'api_callback_url': api_callback_url,
            })
        )

    def test_created(self, returned, created_service_client):
        assert returned == created_service_client

    @pytest.mark.asyncio
    async def test_service_not_found(self, params, returned_func):
        params['service_id'] += 1
        with pytest.raises(ServiceNotFoundError):
            await returned_func()
