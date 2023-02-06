from datetime import timedelta

import pytest

from mail.payments.payments.taskq.workers.transaction_updater import TransactionUpdater, UpdateTransactionAction
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def action_mock(mock_action):
    return mock_action(UpdateTransactionAction)


@pytest.fixture
async def transaction_updater(app, test_logger):
    transaction_updater = TransactionUpdater(logger=test_logger)
    await transaction_updater.initialize_worker(app)
    yield transaction_updater
    transaction_updater.heartbeat_task.cancel()


class TestTransactionUpdater:
    @pytest.fixture
    async def ready_transaction(self, storage, transaction):
        transaction.check_at = utcnow() - timedelta(days=1)
        return await storage.transaction.save(transaction)

    @pytest.mark.asyncio
    async def test_no_task(self, transaction_updater):
        assert not await transaction_updater.process_task()

    @pytest.mark.asyncio
    async def test_no_new_task(self, storage, transaction, transaction_updater):
        transaction.check_at = utcnow() + timedelta(days=1)
        await storage.transaction.save(transaction)
        assert not await transaction_updater.process_task()

    @pytest.mark.asyncio
    async def test_params(self, ready_transaction, transaction_updater, action_mock):
        await transaction_updater.process_task()
        action_mock.assert_called_once_with(transaction=ready_transaction)

    @pytest.mark.asyncio
    async def test_sets_storage(self, ready_transaction, transaction_updater, action_mock):
        await transaction_updater.process_task()
        assert action_mock.context.storage is not None
