from datetime import timedelta

import pytest

from sendr_utils import utcnow

from mail.payments.payments.taskq.workers.abandon_terminator import AbandonTerminator


@pytest.fixture(autouse=True)
def action_mock(mock_action):
    from mail.payments.payments.core.actions.order.abandon_terminate import AbandonTerminateOrderAction
    return mock_action(AbandonTerminateOrderAction)


@pytest.fixture
async def abandon_terminator(app, test_logger):
    worker = AbandonTerminator(logger=test_logger)
    await worker.initialize_worker(app)
    yield worker
    worker.heartbeat_task.cancel()


@pytest.mark.asyncio
async def test_no_pending_refund(abandon_terminator):
    assert not await abandon_terminator.process_task()


class TestAbandonTerminator:
    @pytest.fixture(autouse=True)
    async def setup(self, storage, order):
        order.offline_abandon_deadline = utcnow() - timedelta(minutes=1)
        await storage.order.save(order)

    @pytest.mark.asyncio
    async def test_processed(self, abandon_terminator):
        assert await abandon_terminator.process_task()

    @pytest.mark.asyncio
    async def test_params(self, storage, action_mock, abandon_terminator, order):
        await abandon_terminator.process_task()
        action_mock.assert_called_once_with(order=order, uid=order.uid)
