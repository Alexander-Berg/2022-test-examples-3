import logging

import pytest

from sendr_aiopg.action import BaseDBAction
from sendr_core import BaseAction
from sendr_qlog import LoggerContext
from sendr_taskqueue.worker.storage.action import BaseAsyncDBAction
from sendr_taskqueue.worker.storage.db.entities import TaskState

from .db import StorageContext
from .entities import TaskType


@pytest.fixture
def logger_mock():
    return LoggerContext(logging.getLogger('mock_logger'), {})


@pytest.fixture(autouse=True)
def setup_context(logger_mock, storage_context):
    BaseAction.context = storage_context
    BaseAction.context.storage = None
    BaseAction.context.logger = logger_mock
    BaseDBAction.storage_context_cls = StorageContext


class DummyAsyncAction(BaseAsyncDBAction):
    allow_replica_read = True  # ensure it doesn't break action.run_async()
    action_name = 'dummy'
    task_type = TaskType.MAP


@pytest.mark.asyncio
async def test_storage_is_created_for_async_action(storage):
    action = DummyAsyncAction(foo='bar')
    assert action.context.storage is None

    task = await action.run_async()

    loaded = await storage.task.get(task.task_id)
    assert loaded.task_type == TaskType.MAP
    assert loaded.state == TaskState.PENDING
    assert loaded.params == {'action_kwargs': {'foo': 'bar'}, 'max_retries': 10}
    assert loaded.action_name == 'dummy'
