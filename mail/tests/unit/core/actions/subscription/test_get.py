import pytest

from mail.payments.payments.core.actions.subscription.get import (
    CoreGetSubscriptionAction, GetSubscriptionAction, GetSubscriptionServiceMerchantAction
)
from mail.payments.payments.core.entities.moderation import ModerationData
from mail.payments.payments.core.exceptions import SubscriptionNotFoundError


class TestCoreGetSubscriptionAction:
    @pytest.fixture
    async def customer_subscription(self, storage, customer_subscription):
        customer_subscription.enabled = True
        return await storage.customer_subscription.save(customer_subscription)

    @pytest.fixture
    def returned_func(self, subscription, customer_subscription, merchant):
        async def _inner():
            return await CoreGetSubscriptionAction(
                uid=merchant.uid,
                subscription_id=subscription.subscription_id,
            ).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_returned(self, returned, subscription):
        subscription.moderation = ModerationData(
            approved=False,
            has_moderation=False,
            has_ongoing=False,
        )
        subscription.enabled_customer_subscriptions = 1
        assert returned == subscription

    class TestDeleted:
        @pytest.fixture
        async def subscription(self, storage, subscription):
            subscription.deleted = True
            return await storage.subscription.save(subscription)

        @pytest.mark.asyncio
        async def test_deleted(self, returned_func):
            with pytest.raises(SubscriptionNotFoundError):
                await returned_func()


class TestGetSubscriptionAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreGetSubscriptionAction, subscription)

    @pytest.fixture
    async def returned(self, merchant, subscription):
        return await GetSubscriptionAction(
            uid=merchant.uid,
            subscription_id=subscription.subscription_id,
        ).run()

    def test_returned(self, returned, subscription):
        assert returned == subscription

    def test_core_call_args(self, returned, mock_core_action, merchant, subscription):
        mock_core_action.assert_called_once_with(
            uid=merchant.uid,
            subscription_id=subscription.subscription_id,
        )


class TestGetSubscriptionServiceMerchantAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreGetSubscriptionAction, subscription)

    @pytest.fixture
    async def returned(self, service_merchant, service_client, subscription):
        return await GetSubscriptionServiceMerchantAction(
            service_tvm_id=service_client.tvm_id,
            service_merchant_id=service_merchant.service_merchant_id,
            subscription_id=subscription.subscription_id,
        ).run()

    def test_returned(self, returned, subscription):
        assert returned == subscription

    def test_core_call_args(self, returned, mock_core_action, merchant, subscription):
        mock_core_action.assert_called_once_with(
            uid=merchant.uid,
            subscription_id=subscription.subscription_id,
        )
