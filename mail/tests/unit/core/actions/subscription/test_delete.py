import pytest

from mail.payments.payments.core.actions.subscription.delete import (
    CoreDeleteSubscriptionAction, DeleteSubscriptionAction, DeleteSubscriptionServiceMerchantAction
)
from mail.payments.payments.core.exceptions import SubscriptionNotFoundError
from mail.payments.payments.storage.exceptions import SubscriptionNotFound


class TestCoreDeleteSubscriptionAction:
    @pytest.fixture
    def returned_func(self, subscription):
        async def _inner():
            return await CoreDeleteSubscriptionAction(
                uid=subscription.uid,
                subscription_id=subscription.subscription_id
            ).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_returned(self, returned):
        assert returned.deleted is True

    @pytest.mark.asyncio
    async def test_deleted(self, storage, returned, subscription):
        with pytest.raises(SubscriptionNotFound):
            await storage.subscription.get(uid=subscription.uid, subscription_id=subscription.subscription_id)

    @pytest.mark.asyncio
    async def test_repeated_delete(self, returned, returned_func):
        with pytest.raises(SubscriptionNotFoundError):
            await returned_func()


class TestDeleteSubscriptionAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreDeleteSubscriptionAction, subscription)

    @pytest.fixture
    async def returned(self, merchant, subscription):
        return await DeleteSubscriptionAction(
            uid=merchant.uid,
            subscription_id=subscription.subscription_id
        ).run()

    def test_returned(self, returned, subscription):
        assert returned == subscription

    def test_core_call_args(self, mock_core_action, returned, subscription):
        mock_core_action.assert_called_once_with(
            uid=subscription.uid,
            subscription_id=subscription.subscription_id
        )


class TestDeleteSubscriptionServiceMerchantAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreDeleteSubscriptionAction, subscription)

    @pytest.fixture
    async def returned(self, service_merchant, service_client, subscription):
        return await DeleteSubscriptionServiceMerchantAction(
            service_tvm_id=service_client.tvm_id,
            service_merchant_id=service_merchant.service_merchant_id,
            subscription_id=subscription.subscription_id
        ).run()

    def test_returned(self, returned, subscription):
        assert returned == subscription

    def test_core_call_args(self, mock_core_action, returned, subscription):
        mock_core_action.assert_called_once_with(
            uid=subscription.uid,
            subscription_id=subscription.subscription_id
        )
