import pytest

from mail.payments.payments.core.actions.subscription.get_list import (
    CoreGetSubscriptionListAction, GetSubscriptionListAction, GetSubscriptionListServiceMerchantAction
)


class TestCoreGetSubscriptionListAction:
    @pytest.fixture
    def limit(self):
        return None

    @pytest.fixture
    def offset(self):
        return None

    @pytest.fixture
    def params(self, merchant, offset, limit):
        return {
            'uid': merchant.uid,
            'limit': limit,
            'offset': offset,
        }

    @pytest.fixture
    def action(self, params):
        return CoreGetSubscriptionListAction(**params)

    @pytest.fixture
    def returned_func(self, subscription, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_get(self, subscription, returned):
        assert subscription == returned[0]

    @pytest.mark.parametrize('limit', (0,))
    @pytest.mark.asyncio
    async def test_zero_limit(self, returned):
        assert len(returned) == 0

    @pytest.mark.parametrize('offset', (1,))
    @pytest.mark.asyncio
    async def test_offset(self, returned):
        assert len(returned) == 0

    class TestDeleted:
        @pytest.fixture
        async def subscription(self, subscription, storage):
            subscription.deleted = True
            return await storage.subscription.save(subscription)

        def test_deleted(self, returned):
            assert len(returned) == 0


class TestGetSubscriptionListAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreGetSubscriptionListAction, [subscription])

    @pytest.fixture
    def params(self, merchant):
        return {
            'uid': merchant.uid,
            'limit': 1,
            'offset': 2,
        }

    @pytest.fixture
    async def returned(self, params):
        return await GetSubscriptionListAction(**params).run()

    def test_result(self, returned, subscription):
        assert returned == [subscription]

    def test_core_call_args(self, returned, merchant, mock_core_action):
        mock_core_action.assert_called_once_with(
            uid=merchant.uid,
            limit=1,
            offset=2,
        )


class TestGetSubscriptionListServiceMerchantAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreGetSubscriptionListAction, [subscription])

    @pytest.fixture
    def params(self, service_client, service_merchant):
        return {
            'service_tvm_id': service_client.tvm_id,
            'service_merchant_id': service_merchant.service_merchant_id,
            'limit': 1,
            'offset': 2,
        }

    @pytest.fixture
    async def returned(self, params):
        return await GetSubscriptionListServiceMerchantAction(**params).run()

    def test_result(self, returned, subscription):
        assert returned == [subscription]

    def test_core_call_args(self, returned, merchant, mock_core_action):
        mock_core_action.assert_called_once_with(
            uid=merchant.uid,
            limit=1,
            offset=2,
        )
