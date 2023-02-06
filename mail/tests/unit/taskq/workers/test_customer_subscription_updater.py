from datetime import timedelta

import pytest

from mail.payments.payments.core.entities.enums import PayStatus
from mail.payments.payments.taskq.workers.customer_subscription_updater import CustomerSubscriptionUpdater
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def action_mock(mock_action):
    from mail.payments.payments.taskq.workers.customer_subscription_updater import SyncCustomerSubscriptionAction
    return mock_action(SyncCustomerSubscriptionAction)


@pytest.fixture(autouse=True)
def pause(payments_settings):
    payments_settings.CUSTOMER_SUBSCRIPTION_UPDATER_PAUSE = 0
    return payments_settings.CUSTOMER_SUBSCRIPTION_UPDATER_PAUSE


@pytest.fixture
async def customer_subscription_updater(app, test_logger):
    customer_subscription_updater = CustomerSubscriptionUpdater(logger=test_logger)
    await customer_subscription_updater.initialize_worker(app)
    yield customer_subscription_updater
    customer_subscription_updater.heartbeat_task.cancel()


class TestCustomerSubscriptionUpdater:
    @pytest.mark.asyncio
    async def test_no_task(self, customer_subscription_updater):
        assert not await customer_subscription_updater.process_task()

    @pytest.mark.asyncio
    async def test_no_new_task(self, storage, customer_subscription, customer_subscription_updater):
        customer_subscription.time_until = utcnow()
        await storage.customer_subscription.save(customer_subscription)
        assert not await customer_subscription_updater.process_task()

    class TestContext:
        @pytest.fixture
        def pay_status(self):
            return PayStatus.PAID

        @pytest.fixture
        def lag(self, payments_settings):
            return payments_settings.CUSTOMER_SUBSCRIPTION_CALLBACK_LAG

        @pytest.fixture(autouse=True)
        async def setup(self, storage, lag, customer_subscription, order_with_customer_subscription, pay_status):
            order_with_customer_subscription.pay_status = pay_status
            await storage.order.save(order_with_customer_subscription)

            customer_subscription.time_until = utcnow() - timedelta(seconds=lag + 60)
            await storage.customer_subscription.save(customer_subscription)

        @pytest.mark.asyncio
        async def test_context_storage(self, customer_subscription, customer_subscription_updater, action_mock):
            await customer_subscription_updater.process_task()
            assert action_mock.context.storage is not None

        @pytest.mark.asyncio
        async def test_context(self, customer_subscription, customer_subscription_updater, action_mock):
            await customer_subscription_updater.process_task()
            action_mock.assert_called_once_with(
                uid=customer_subscription.uid,
                order_id=customer_subscription.order_id,
            )

        @pytest.mark.parametrize('pay_status', (PayStatus.NEW,))
        @pytest.mark.asyncio
        async def test_context_false_by_pay_status(self, customer_subscription_updater):
            assert await customer_subscription_updater.process_task() is False

        @pytest.mark.parametrize('lag', (0,))
        @pytest.mark.asyncio
        async def test_context_false_by_lag(self, customer_subscription_updater):
            assert await customer_subscription_updater.process_task() is False
