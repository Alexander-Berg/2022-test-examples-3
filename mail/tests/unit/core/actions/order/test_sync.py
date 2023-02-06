import pytest

from mail.payments.payments.core.actions.customer_subscription.sync import SyncCustomerSubscriptionAction
from mail.payments.payments.core.actions.order.sync import SyncOrderAction, UpdateTransactionAction
from mail.payments.payments.core.entities.transaction import Transaction, TransactionStatus


class BaseTestSyncOrderAction:
    @pytest.fixture(autouse=True)
    def update_transaction_calls(self, mock_action):
        return mock_action(UpdateTransactionAction).call_args_list

    @pytest.fixture
    def params(self, merchant, order):
        return {
            'uid': merchant.uid,
            'order_id': order.order_id,
        }

    @pytest.fixture
    async def setup_tx(self, storage, order):
        kwargs = dict(uid=order.uid, order_id=order.order_id)
        await storage.transaction.create(Transaction(**kwargs, status=TransactionStatus.FAILED))
        return await storage.transaction.create(Transaction(**kwargs, status=TransactionStatus.ACTIVE))

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await SyncOrderAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_update_transaction_call(self, update_transaction_calls, setup_tx, returned):
        assert all((
            len(update_transaction_calls) == 1,
            update_transaction_calls[0][1] == {'transaction': setup_tx},
        ))

    @pytest.mark.asyncio
    async def test_without_transactions(self, update_transaction_calls, returned):
        assert update_transaction_calls == []

    @pytest.mark.asyncio
    async def test_transaction_finished(self, storage, update_transaction_calls, setup_tx, returned_func):
        setup_tx.status = TransactionStatus.CANCELLED
        await storage.transaction.save(setup_tx)
        await returned_func()
        assert update_transaction_calls == []


class TestSyncOrderAction(BaseTestSyncOrderAction):
    pass


@pytest.mark.usefixtures('order_with_customer_subscription')
class TestSyncOrderWithCustomerSubscriptionAction(BaseTestSyncOrderAction):
    @pytest.fixture(autouse=True)
    def sync_customer_subscription_mock(self, mock_action):
        return mock_action(SyncCustomerSubscriptionAction)

    @pytest.mark.asyncio
    async def test_sync_customer_subscription_call(self, order, sync_customer_subscription_mock, setup_tx, returned):
        sync_customer_subscription_mock.assert_called_once_with(
            uid=order.uid,
            order_id=order.order_id,
        )
