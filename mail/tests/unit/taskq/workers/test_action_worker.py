from datetime import timedelta

import pytest

from mail.payments.payments.core.actions.order.clear_unhold import CoreClearUnholdOrderAction
from mail.payments.payments.core.entities.enums import TaskState
from mail.payments.payments.taskq.workers.action_worker import ActionWorker


class TestActionWorker:
    @pytest.fixture
    async def worker(self, app, test_logger, loop):
        worker = ActionWorker(logger=test_logger)
        await worker.initialize_worker(app)
        yield worker
        worker.heartbeat_task.cancel()

    @pytest.mark.asyncio
    async def test_update_stats(self, worker, app):
        await worker.update_stats(app)

    class TestIgnoreOrderPayStatusMustBeHeldOrInModerationError:
        @pytest.fixture(params=('clear', 'unhold'))
        def operation(self, request):
            return request.param

        @pytest.fixture
        def order_data(self, order_data):
            return {
                'autoclear': False,
                **order_data
            }

        @pytest.fixture
        def params(self, order, positive_merchant_moderation, operation):
            return {
                'uid': order.uid,
                'order_id': order.order_id,
                'operation': operation,
            }

        @pytest.fixture(autouse=True)
        def update_transaction_mock(self, mock_action, transaction):
            from mail.payments.payments.core.actions.update_transaction import UpdateTransactionAction
            yield mock_action(UpdateTransactionAction, transaction)

        @pytest.fixture
        def action(self, params):
            return CoreClearUnholdOrderAction(**params)

        @pytest.fixture
        def returned_func(self, action, storage):
            async def _inner():
                task = await action.run_async()
                task.run_at -= timedelta(hours=1)
                return await storage.task.save(task)

            return _inner

        @pytest.mark.asyncio
        async def test_ignore_exception(self, storage, returned, worker, action):  # PAYBACK-919
            result = await worker.process_task()
            task = await storage.task.get(returned.task_id)

            assert all((
                result,
                task.state == TaskState.FINISHED
            ))
