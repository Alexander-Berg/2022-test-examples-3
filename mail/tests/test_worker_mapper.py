import uuid
from datetime import timedelta

import pytest

from sendr_taskqueue.worker.storage import Worker, WorkerState
from sendr_taskqueue.worker.storage.db.entities import Task, TaskState
from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains_inanyorder

from .db import TaskType, WorkerType


@pytest.mark.asyncio
async def test_can_delete_workers(storage):
    task = await storage.task.create(
        Task(
            state=TaskState.PENDING,
            task_type=TaskType.MAP,
            params={},
        )
    )
    workers = (
        Worker(
            worker_id=str(uuid.uuid4()),
            worker_type=WorkerType.MAPPER,
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365)
        ),
        Worker(
            worker_id=str(uuid.uuid4()),
            worker_type=WorkerType.MAPPER,
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365),
            task_id=task.task_id,
        ),
        Worker(
            worker_id=str(uuid.uuid4()),
            worker_type=WorkerType.MAPPER,
            host='',
            state=WorkerState.RUNNING,
            heartbeat=utcnow() - timedelta(days=365)
        ),
        Worker(
            worker_id=str(uuid.uuid4()),
            worker_type=WorkerType.MAPPER,
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow()
        ),
    )
    created_workers = []
    for worker in workers:
        created_workers.append(await storage.worker.create(worker))

    deleted_count = await storage.worker.delete_cleaned_up_workers(
        batch_size=10, period_offset=timedelta(days=1)
    )

    expected_left_workers = [
        created_workers[1],
        created_workers[2],
        created_workers[3],
    ]
    workers_from_base = await alist(storage.worker.find(states=list(WorkerState)))
    assert deleted_count == 1
    assert_that(workers_from_base, contains_inanyorder(*expected_left_workers))

    zero_deleted_count = await storage.worker.delete_cleaned_up_workers(
        batch_size=10, period_offset=timedelta(days=1)
    )
    assert zero_deleted_count == 0


@pytest.mark.asyncio
async def test_delete_is_limited_by_batch_size(storage):
    workers = (
        Worker(
            worker_id=str(uuid.uuid4()),
            worker_type=WorkerType.MAPPER,
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365)
        ),
        Worker(
            worker_id=str(uuid.uuid4()),
            worker_type=WorkerType.MAPPER,
            host='',
            state=WorkerState.CLEANEDUP,
            heartbeat=utcnow() - timedelta(days=365)
        ),
    )
    for workers in workers:
        await storage.worker.create(workers)

    deleted_count = await storage.worker.delete_cleaned_up_workers(
        batch_size=1,
        period_offset=timedelta(days=1)
    )
    assert deleted_count == 1

    deleted_count = await storage.worker.delete_cleaned_up_workers(
        batch_size=1,
        period_offset=timedelta(days=1)
    )
    assert deleted_count == 1

    deleted_count = await storage.worker.delete_cleaned_up_workers(
        batch_size=1,
        period_offset=timedelta(days=1)
    )
    assert deleted_count == 0
