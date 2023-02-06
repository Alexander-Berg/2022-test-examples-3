import datetime

import pytest

from hamcrest import assert_that, equal_to, not_

from mail.ipa.ipa.core.actions.collectors.check import CheckCollectorStatusAction
from mail.ipa.ipa.taskq.collector import CollectorWorker


@pytest.fixture
def worker(worker_app, test_logger):
    worker = CollectorWorker(logger=test_logger)
    worker.app = worker_app
    return worker


@pytest.fixture(autouse=True)
def mock_check(mock_action, task):
    return mock_action(CheckCollectorStatusAction, task)


@pytest.fixture
async def process_task(worker):
    return await worker.process_task()


@pytest.fixture
def past_time():
    return datetime.datetime(2000, 1, 1, 0, 0, 0, tzinfo=datetime.timezone.utc)


class TestCollectorWorker_Success:
    @pytest.fixture(autouse=True)
    async def task(self, storage, create_collector, user, past_time):
        collector = await create_collector(user_id=user.user_id, enabled=True)
        collector.pop_id = 'pop_id'
        collector.checked_at = past_time
        collector.modified_at = past_time
        return await storage.collector.save(collector, update_modified_at=False)

    def test_returned(self, process_task, worker):
        assert process_task == worker.PROCESS_TASK_WITH_NO_PAUSE

    def test_check_called(self, mock_check, process_task):
        mock_check.assert_called_once()

    @pytest.mark.asyncio
    async def test_checked_at_updated(self, process_task, task, storage, past_time):
        actual_task = await storage.collector.get(task.collector_id)
        assert_that(actual_task.checked_at, not_(equal_to(past_time)))

    @pytest.mark.asyncio
    async def test_modified_at_not_updated(self, process_task, task, storage, past_time):
        actual_task = await storage.collector.get(task.collector_id)
        assert_that(actual_task.modified_at, equal_to(past_time))


class FailedGetTaskContract:
    def test_returned(self, process_task, worker):
        assert process_task == worker.PROCESS_TASK_WITH_PAUSE

    def test_check_called(self, mock_check, process_task):
        mock_check.assert_not_called()


class TestCollectorWorker_NoCollectorsOn(FailedGetTaskContract):
    @pytest.fixture(autouse=True)
    async def task(self, create_collector, user):
        return await create_collector(user_id=user.user_id, enabled=False, pop_id='pop_id')


class TestCollectorWorker_NoCollectorsWithPopID(FailedGetTaskContract):
    @pytest.fixture(autouse=True)
    async def task(self, create_collector, user):
        return await create_collector(user_id=user.user_id, enabled=True, pop_id=None)
