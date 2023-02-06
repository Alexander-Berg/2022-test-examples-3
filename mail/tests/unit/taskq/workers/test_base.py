from datetime import timedelta

import pytest

from sendr_taskqueue import BaseActionStorageWorker

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.actions.base.db import BaseDBAction
from mail.payments.payments.core.entities.enums import TaskState, TaskType, WorkerType
from mail.payments.payments.core.entities.task import Task
from mail.payments.payments.core.exceptions import BaseCoreError, CoreInteractionResponseError
from mail.payments.payments.taskq.workers.base import BaseWorker


class Action(BaseDBAction):
    action_name = 'action'

    def __init__(self, a: int, b: str):
        super().__init__()
        self.a = a
        self.b = b

    async def handle(self):
        pass


class Worker(BaseWorker, BaseActionStorageWorker):
    task_type = TaskType.RUN_ACTION
    worker_type = WorkerType.ACTION_EXECUTOR
    actions = (Action,)

    async def heartbeat(self):
        pass


@pytest.fixture
async def worker(app, test_logger):
    worker = Worker(logger=test_logger)
    await worker.initialize_worker(app)
    yield worker
    worker.heartbeat_task.cancel()


@pytest.fixture
def action_kwargs():
    return {'a': 1, 'b': 'b'}


@pytest.fixture
def max_retries():
    return 1


@pytest.fixture
async def task(storage, max_retries, action_kwargs):
    """Scheduled Action in form of Task"""
    Action.max_retries = max_retries
    task = await Action(**action_kwargs).run_async()
    task.run_at -= timedelta(hours=1)  # prevent strange small time difference of creation and fetching
    task.details = {}
    return await storage.task.save(task)


@pytest.fixture
async def changed_action_task(task, storage):
    task.action_name = task.action_name + '_other'
    return await storage.task.save(task)


@pytest.mark.asyncio
async def test_worker_check_action_name(mocker, test_logger):
    mocker.patch.object(Action, 'action_name', None)
    with pytest.raises(RuntimeError):
        Worker(logger=test_logger)


def test_worker_action_mapping(worker):
    assert dict(worker.action_mapping.items()) == {Action.action_name: Action}


def test_get_action_method_returns_correct_action(worker, task):
    assert worker.get_action_class(task) is Action


def test_get_params_method_requires_task(worker):
    with pytest.raises(AssertionError):
        worker.get_params()


@pytest.mark.asyncio
async def test_can_fetch_task_from_storage(task, worker, storage):
    fetched = await worker.fetch_task_for_work(storage)
    assert fetched == task


@pytest.mark.asyncio
async def test_unable_to_fetch_task_for_other_action(changed_action_task, worker, storage):
    """Action name is important to fetch task - same task but with changed action name should be not fetched"""
    with pytest.raises(Task.DoesNotExist):
        await worker.fetch_task_for_work(storage)


def test_context_contains_task_params(task, worker):
    context = worker.get_params(task)
    assert_that(context, has_entries(task.params.action_kwargs))


class BaseTest:
    @pytest.fixture
    async def processed(self, worker, task):
        await worker.process_task()

    @pytest.fixture
    async def task_processed(self, processed, task, storage):
        return await storage.task.get(task_id=task.task_id)

    @pytest.fixture(autouse=True)
    def worker_mock(self, mocker):
        mocker.spy(Worker, 'task_retry')
        mocker.spy(Worker, 'task_done')
        mocker.spy(Worker, 'task_fail')


class TestProcessTask(BaseTest):
    @pytest.fixture(autouse=True)
    def mocks(self, mocker):
        mocker.spy(Action, '__init__')
        mocker.spy(Action, 'handle')

    @pytest.mark.asyncio
    async def test_action_handle_method_called(self, processed):
        Action.handle.assert_called_once()

    @pytest.mark.asyncio
    async def test_action_init_params(self, processed, task):
        provided_context = Action.__init__.call_args[1]
        assert_that(provided_context, has_entries(**task.params['action_kwargs']))

    @pytest.mark.asyncio
    async def test_task_finished(self, processed, task, storage, task_processed):
        assert task_processed.state == TaskState.FINISHED

    @pytest.mark.asyncio
    async def test_task_done_called(self, processed):
        Worker.task_done.assert_called_once()


class TestActionException(BaseTest):
    @pytest.fixture
    def action_exception(self):
        return CoreInteractionResponseError()

    @pytest.fixture
    def retry_exceptions(self):
        return CoreInteractionResponseError,

    @pytest.fixture(autouse=True)
    def retry_exceptions_mock(self, mocker, retry_exceptions):
        mocker.patch.object(Worker, 'retry_exceptions', retry_exceptions)

    @pytest.fixture(autouse=True)
    def raising_handle(self, mocker, action_exception):
        async def handle(self):
            raise action_exception

        mocker.patch.object(Action, 'handle', handle)

    @pytest.mark.asyncio
    async def test_task_retried_and_task_state_is_pending(self, processed, task_processed):
        assert_that(task_processed, has_properties({
            'state': TaskState.PENDING,
            'retries': 1,
        }))

    @pytest.mark.parametrize('action_exception', [BaseCoreError])
    @pytest.mark.asyncio
    async def test_task_state_failed_on_bad_exception(self, action_exception, processed, task_processed):
        assert_that(task_processed, has_properties({
            'state': TaskState.FAILED,
            'retries': 0,
        }))

    @pytest.mark.parametrize('max_retries', [0])
    @pytest.mark.asyncio
    async def test_task_failed_because_max_retries_exceeded(self, processed, max_retries, task_processed):
        """Respects max_retries argument of run_async"""
        assert_that(task_processed, has_properties({
            'state': TaskState.FAILED,
            'retries': 0,
        }))
