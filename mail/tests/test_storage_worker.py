import pytest

from sendr_core.action import BaseAction
from sendr_taskqueue.worker.storage import BaseStorageWorker
from sendr_taskqueue.worker.storage.db.entities import Task, TaskState, WorkerState
from sendr_taskqueue.worker.storage.exceptions import WorkerShutdownError

from .db import StorageContext, TaskType, WorkerType


class MapAction(BaseAction):
    pass


class StorageMapWorker(BaseStorageWorker):
    worker_type = WorkerType.MAPPER
    storage_context_cls = StorageContext
    task_action_mapping = {
        TaskType.MAP: MapAction,
    }


@pytest.fixture
def app(mocker, aiopg_engine):
    return mocker.NonCallableMock(db_engine=aiopg_engine)


@pytest.fixture
async def worker(app, logger):
    # once=True нужен, чтобы можно было вызвать worker.__call__.
    # Для более точечных тестов может понадобится worker.run()
    # или даже лучше worker.process_task().
    worker = StorageMapWorker(logger=logger, once=True)

    # Обычно initialize_worker выполняется в BaseWorkerApplication.
    # Но я бы отдельно протестил связку Application <-> Schedule <-> worker.
    # Частные случаи использования worker'а так тестировать слишком сложно. Или нет?
    await worker.initialize_worker(app)
    yield worker

    # Если вызывать worker.__call__, то _cleanup вызывать не обязательно: он вызовется сам.
    # Но лучше чтобы эта строчка была тут. Не всегда необходимо вызывать именно __call__.
    # Если вдруг вызов _cleanup станет неидемпотентным, придётся эту строчку убрать.
    await worker._cleanup()


@pytest.fixture
async def worker_entity(worker):
    return worker.worker


@pytest.fixture
async def task(storage):
    return await storage.task.create(
        Task(
            state=TaskState.PENDING,
            task_type=TaskType.MAP,
            params={},
        )
    )


@pytest.fixture(autouse=True)
def action_run_mock(mocker, coromock):
    return mocker.patch.object(MapAction, 'run', coromock())


class TestWorkerFinishesTask:
    @pytest.fixture(autouse=True)
    async def run(self, worker, task, app):
        await worker(app)

    @pytest.mark.asyncio
    async def test_worker_finishes_task(self, storage, task):
        task = await storage.task.get(task.task_id)
        assert task.state == TaskState.FINISHED

    @pytest.mark.asyncio
    async def test_action_called(self, action_run_mock):
        action_run_mock.assert_called_once()


class TestCleanedUpWorkerDoesNotTakeTask:
    @pytest.fixture(autouse=True)
    async def worker_entity(self, worker_entity, storage):
        worker_entity.state = WorkerState.CLEANEDUP
        return await storage.worker.save(worker_entity)

    @pytest.mark.asyncio
    async def test_raises(self, worker, task, app):
        with pytest.raises(WorkerShutdownError):
            await worker(app)

    class TestObjects:
        @pytest.fixture(autouse=True)
        async def run(self, worker, task, app):
            with pytest.raises(WorkerShutdownError):
                await worker(app)

        @pytest.mark.asyncio
        async def test_task_is_pending(self, storage, task):
            task = await storage.task.get(task.task_id)
            assert task.state == TaskState.PENDING

        @pytest.mark.asyncio
        async def test_task_has_no_retries(self, storage, task):
            task = await storage.task.get(task.task_id)
            assert task.retries == 0

        @pytest.mark.asyncio
        async def test_action_not_called(self, action_run_mock):
            action_run_mock.assert_not_called()
