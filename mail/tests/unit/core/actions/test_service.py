import pytest

from mail.payments.payments.core.actions.service import (
    CreateServiceAction, GetServiceListAction, GetServiceListByServiceMerchantsAction
)


class TestServiceCreate:
    @pytest.fixture
    def tvm_id(self, randn, unique_rand):
        return unique_rand(randn, basket='tvm_id')

    @pytest.fixture
    def api_callback_url(self):
        return 'http://service.domain'

    @pytest.fixture
    def name(self):
        return 'service name'

    @pytest.fixture
    def order_moderation_enabled(self):
        return True

    @pytest.fixture
    def hidden(self):
        return False

    @pytest.fixture
    def antifraud(self):
        return True

    @pytest.fixture
    def slug(self):
        return 'the-slug'

    @pytest.fixture
    def params(self, tvm_id, api_callback_url, name, slug, order_moderation_enabled, hidden, antifraud):
        return {
            'tvm_id': tvm_id,
            'slug': slug,
            'api_callback_url': api_callback_url,
            'name': name,
            'order_moderation_enabled': order_moderation_enabled,
            'antifraud': antifraud,
            'hidden': hidden,
        }

    @pytest.fixture
    async def returned(self, params):
        return await CreateServiceAction(**params).run()

    @pytest.mark.asyncio
    async def test_creates_service(self, storage, returned):
        service = await storage.service.get(returned.service_id)
        assert service == returned


class TestGetServiceListAction:
    @pytest.fixture
    def hidden(self):
        return False

    @pytest.fixture
    async def returned(self, hidden):
        return await GetServiceListAction(hidden=hidden).run()

    @pytest.mark.asyncio
    async def test_get_service_list(self, storage, service, returned):
        assert returned == [service]

    class TestHidden:
        @pytest.fixture(autouse=True)
        async def setup(self, storage, service):
            service.hidden = True
            return await storage.service.save(service)

        @pytest.mark.asyncio
        async def test_hidden_false(self, storage, service, returned):
            assert returned == []

        @pytest.mark.parametrize('hidden', (True,))
        @pytest.mark.asyncio
        async def test_hidden_true(self, storage, service, hidden, returned):
            assert returned == [service]


class TestGetServiceListByServiceMerchantsAction:
    @pytest.fixture
    async def returned(self, storage, service_merchant):
        return await GetServiceListByServiceMerchantsAction(uid=service_merchant.uid).run()

    @pytest.mark.asyncio
    async def test_find_by_service_merchants(self, returned, service):
        assert returned == [service]
