from datetime import timedelta

import pytest

from sendr_utils import utcnow

from mail.ipa.ipa.core.entities.enums import TaskState, TaskType, WorkerState
from mail.ipa.ipa.taskq.arbiter import ArbiterWorker
from mail.ipa.ipa.utils.stats import collector_check_period_gauge, queue_size_gauge, queue_tasks_gauge


@pytest.fixture
def arbiter(worker_app, test_logger):
    arbiter = ArbiterWorker(logger=test_logger, workers=())
    arbiter.app = worker_app
    return arbiter


def get_metric_value(metric):
    metrics_tuple = metric.get()
    _, value = metrics_tuple[0]
    return value


class TestArbiterCountTasks:
    @pytest.fixture
    async def returned(self, arbiter, storage):
        return await arbiter.count_tasks(storage)

    class TestArbiterTaskCountCollectorCheckPeriod:
        @pytest.fixture(autouse=True)
        async def collector(self, user, storage, create_collector, pop_id):
            return await create_collector(user_id=user.user_id,
                                          checked_at=utcnow() - timedelta(minutes=10),
                                          pop_id=pop_id,
                                          enabled=True,
                                          )

        def test_collector_check_period(self, returned):
            value = get_metric_value(collector_check_period_gauge)
            assert timedelta(minutes=9.75).total_seconds() <= value <= timedelta(minutes=10.25).total_seconds()

    class TestArbiterTaskCountQueue:
        @pytest.fixture(autouse=True)
        async def tasks(self, org_id, create_task):
            await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.FINISHED)
            await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.PENDING)
            await create_task(entity_id=org_id, task_type=TaskType.INIT_IMPORT, state=TaskState.PENDING)
            await create_task(entity_id=org_id, task_type=TaskType.INIT_IMPORT, state=TaskState.PENDING)
            await create_task(entity_id=org_id, task_type=TaskType.INIT_USER_IMPORT, state=TaskState.FAILED)

        @pytest.mark.asyncio
        async def test_queue_tasks(self, returned):
            assert 1 == get_metric_value(queue_tasks_gauge.labels(TaskType.PARSE_CSV.value))
            assert 2 == get_metric_value(queue_tasks_gauge.labels(TaskType.INIT_IMPORT.value))
            assert 0 == get_metric_value(queue_tasks_gauge.labels(TaskType.INIT_USER_IMPORT.value))

        @pytest.mark.asyncio
        async def test_queue_size(self, returned):
            assert 3 == get_metric_value(queue_size_gauge)


class TestArbiterCleanTasks:
    @pytest.fixture(autouse=True)
    async def task(self, org_id, create_task):
        return await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.PROCESSING)

    @pytest.fixture(autouse=True)
    async def worker(self, org_id, create_worker, task):
        return await create_worker(heartbeat=utcnow() - timedelta(days=1), task_id=task.task_id)

    @pytest.fixture(autouse=True)
    async def run_clean_tasks(self, arbiter, task, worker):
        await arbiter.clean_tasks(arbiter.app)

    @pytest.mark.asyncio
    async def test_worker_cleaned_up(self, storage, worker):
        worker = await storage.worker.get(worker.worker_id)
        assert worker.state == WorkerState.CLEANEDUP
        assert worker.task_id is None

    @pytest.mark.asyncio
    async def test_task_cleaned_up(self, storage, task):
        task = await storage.task.get(task.task_id)
        assert task.state == TaskState.PENDING
