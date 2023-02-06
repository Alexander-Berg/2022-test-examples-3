from datetime import datetime, timezone
from uuid import uuid4

import pytest

from sendr_utils import alist

from mail.payments.payments.core.entities.enums import WorkerState, WorkerType
from mail.payments.payments.core.entities.worker import Worker
from mail.payments.payments.storage.db.tables import metadata


@pytest.fixture
def worker():
    return Worker(
        worker_id=uuid4().hex,
        worker_type=WorkerType.CALLBACK_SENDER,
        host='local',
        state=WorkerState.RUNNING,
        startup=datetime.now(tz=timezone.utc),
    )


def test_map(storage, worker):
    worker.task_id = 432
    worker_dict = {
        attr: getattr(worker, attr)
        for attr in [
            'worker_id',
            'worker_type',
            'host',
            'state',
            'heartbeat',
            'startup',
            'task_id',
        ]
    }
    assert storage.worker.map(worker_dict) == worker


def test_unmap(storage, worker):
    worker.task_id = 432
    assert storage.worker.unmap(worker) == {
        attr: getattr(worker, attr)
        for attr in [
            'state',
            'task_id',
        ]
    }


@pytest.mark.asyncio
async def test_create_writes(storage, worker):
    await storage.worker.create(worker)
    result = await storage.conn.execute(f'SELECT worker_id FROM {metadata.schema}.workers;')
    assert (await result.fetchone())[0] == worker.worker_id


@pytest.mark.asyncio
async def test_create_returns(storage, worker):
    returned = await storage.worker.create(worker)
    assert returned.worker_id == worker.worker_id


@pytest.mark.asyncio
async def test_find_without_params(storage):
    with pytest.raises(AssertionError):
        await alist(storage.worker.find())


@pytest.mark.asyncio
async def test_find_state(storage, worker):
    worker = await storage.worker.create(worker)
    workers = await alist(storage.worker.find(state=worker.state))
    assert workers == [worker]


@pytest.mark.asyncio
async def test_find_states(storage, worker):
    worker = await storage.worker.create(worker)
    workers = await alist(storage.worker.find(states=[worker.state]))
    assert workers == [worker]


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Worker.DoesNotExist):
        await storage.worker.get(uuid4().hex)


@pytest.mark.asyncio
async def test_get(storage, worker):
    worker = await storage.worker.create(worker)
    assert await storage.worker.get(worker_id=worker.worker_id) == worker


@pytest.mark.asyncio
async def test_heartbeat(storage, worker):
    await storage.worker.create(worker)
    assert await storage.worker.heartbeat(worker_id=worker.worker_id) == worker.worker_id


@pytest.mark.asyncio
async def test_save_returns_worker(storage, worker):
    worker = await storage.worker.create(worker)
    worker.state = WorkerState.SHUTDOWN
    assert await storage.worker.save(worker) == worker
