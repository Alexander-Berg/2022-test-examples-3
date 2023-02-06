from datetime import timedelta

import pytest

from hamcrest import assert_that, equal_to

from sendr_utils import utcnow

from mail.ipa.ipa.core.entities.enums import WorkerState


@pytest.mark.asyncio
async def test_delete_workers(storage, create_worker, create_task):
    task = await create_task()
    not_so_old = await create_worker(state=WorkerState.CLEANEDUP, heartbeat=utcnow() - timedelta(days=13))
    await create_worker(state=WorkerState.CLEANEDUP, heartbeat=utcnow() - timedelta(days=15))
    running_old = await create_worker(state=WorkerState.RUNNING, heartbeat=utcnow() - timedelta(days=15))
    old_with_task = await create_worker(state=WorkerState.CLEANEDUP, heartbeat=utcnow() - timedelta(days=15), task_id=task.task_id)

    await storage.worker.clean_old_workers(last_heartbeat_age=timedelta(days=14))

    assert_that(
        sorted(
            [worker.worker_id async for worker in storage.worker.find(states=(WorkerState.RUNNING, WorkerState.CLEANEDUP))]
        ),
        equal_to(sorted([not_so_old.worker_id, running_old.worker_id, old_with_task.worker_id])),
    )
