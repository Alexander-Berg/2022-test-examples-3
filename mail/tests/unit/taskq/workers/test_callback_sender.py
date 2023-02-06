from datetime import timedelta

import pytest

from sendr_interactions import exceptions as interaction_errors
from sendr_taskqueue.worker.storage import TaskState
from sendr_utils import utcnow

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.actions.worker.callback import CallbackWorkerAction
from mail.payments.payments.core.entities.enums import TaskType
from mail.payments.payments.core.entities.task import Task
from mail.payments.payments.core.exceptions import CoreInteractionConnectionTimeoutError
from mail.payments.payments.taskq.workers.callback_sender import CallbackSender
from mail.payments.payments.tests.utils import dummy_async_function


@pytest.fixture
async def callback_sender(app, test_logger):
    callback_sender = CallbackSender(logger=test_logger)
    await callback_sender.initialize_worker(app)
    yield callback_sender
    callback_sender.heartbeat_task.cancel()


@pytest.fixture
def action_result():
    return None


@pytest.fixture
def action_exc():
    return None


@pytest.fixture(autouse=True)
def callback_worker_action_mock(mocker, noop, action_result, action_exc):
    mocker.patch.object(CallbackWorkerAction, '__init__', noop)
    mocker.patch.object(CallbackWorkerAction, 'run', dummy_async_function(result=action_result, exc=action_exc))


def test_get_params(rands, callback_sender):
    task = Task(task_type=TaskType.API_CALLBACK, params={rands(): rands()})
    params = callback_sender.get_params(task)
    assert_that(params, has_entries(**task.params))


class TestRetries:
    @pytest.fixture(params=(
        CoreInteractionConnectionTimeoutError,
        interaction_errors.InteractionResponseError(
            service='callback',
            method='GET',
            status_code=500,
            response_status='error',
        )
    ))
    def action_exc(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def setup(self, randn):
        CallbackSender.max_retries = randn(min=10, max=20)

    @pytest.mark.asyncio
    @pytest.mark.parametrize('delta,state', ((0, TaskState.PENDING), (1, TaskState.FAILED)))
    async def test_retries(self, callback_sender, storage, delta, state):
        task = Task(task_type=TaskType.API_CALLBACK, params={}, run_at=utcnow() - timedelta(days=1))
        task = await storage.task.create(task)

        for _ in range(CallbackSender.max_retries + delta):
            await callback_sender.process_task()

            task = await storage.task.get(task.task_id)
            task.run_at = utcnow() - timedelta(days=1)
            task = await storage.task.save(task)

        task = await storage.task.get(task.task_id)
        assert task.state == state
