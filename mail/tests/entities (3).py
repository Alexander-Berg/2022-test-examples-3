import pytest

from sendr_taskqueue.worker.storage.db.entities import Worker

from mail.ipa.ipa.core.entities.collector import Collector
from mail.ipa.ipa.core.entities.enums import EventType, TaskState, TaskType, WorkerType, WorkerState
from mail.ipa.ipa.core.entities.event import Event
from mail.ipa.ipa.core.entities.import_params import GeneralImportParams, GeneralInitImportParams, ImportParams
from mail.ipa.ipa.core.entities.password import Password
from mail.ipa.ipa.core.entities.task import Task
from mail.ipa.ipa.core.entities.user import User
from mail.ipa.ipa.core.entities.user_info import UserInfo
from mail.ipa.ipa.interactions.yarm import YarmCollector, YarmCollectorStatus
from mail.ipa.ipa.interactions.yarm.entities import YarmCollectorFolderStatus, YarmCollectorState


@pytest.fixture
def import_params(randn, rands):
    return ImportParams(
        server=rands(),
        port=randn(),
        ssl=True,
        imap=True,
        mark_archive_read=True,
        delete_msgs=True,
        src_login=rands(),
    )


@pytest.fixture
def create_user(storage, rands):
    async def _inner(org_id, login=None, error=None, **kwargs):
        return await storage.user.create(User(
            org_id=org_id,
            login=login or rands(),
            error=error,
            **kwargs,
        ))

    return _inner


@pytest.fixture
def create_task(storage, past_time):
    async def _inner(task_type=TaskType.PARSE_CSV, entity_id=None, state=TaskState.PENDING, run_at=past_time, **kwargs):
        return await storage.task.create(
            Task(
                task_type=task_type,
                entity_id=entity_id,
                run_at=run_at,
                state=state,
                **kwargs,
            )
        )

    return _inner


@pytest.fixture
def create_worker(storage, past_time, rands):
    async def _inner(worker_type=WorkerType.USER, entity_id=None, state=WorkerState.RUNNING, heartbeat=past_time, **kwargs):
        return await storage.worker.create(
            Worker(
                worker_type=worker_type,
                worker_id=rands(),
                host=rands(),
                heartbeat=heartbeat,
                state=state,
                **kwargs,
            )
        )

    return _inner


@pytest.fixture
def create_event(storage, org_id):
    async def _inner(event_type=EventType.START, org_id=org_id, revision=1, **kwargs):
        return await storage.event.create(
            Event(
                event_type=event_type,
                org_id=org_id,
                revision=revision,
                **kwargs,
            )
        )

    return _inner


@pytest.fixture
async def user(org_id, create_user):
    return await create_user(org_id)


@pytest.fixture
async def existing_user(org_id, create_user, uid, suid):
    return await create_user(org_id, uid=uid, suid=suid)


@pytest.fixture
def create_collector(storage, import_params):
    async def _inner(user_id, params=import_params, with_user=False, **kwargs):
        collector = await storage.collector.create(
            Collector(
                user_id=user_id,
                params=params,
                **kwargs
            )
        )
        for key in kwargs:
            setattr(collector, key, kwargs[key])

        collector = await storage.collector.save(collector)
        if with_user:
            collector = await storage.collector.get(collector.collector_id, with_user=True)
        return collector

    return _inner


@pytest.fixture
async def organization(storage, org_id):
    return await storage.organization.get_or_create(org_id)


@pytest.fixture
def general_import_params():
    return GeneralImportParams(
        server='server.test',
        port=993,
        imap=False,
        ssl=True,
        mark_archive_read=False,
        delete_msgs=True,
    )


@pytest.fixture
def general_init_import_params(general_import_params, org_id, admin_uid, user_ip):
    return GeneralInitImportParams(
        org_id=org_id,
        admin_uid=admin_uid,
        user_ip=user_ip,
        **general_import_params.as_dict()
    )


@pytest.fixture
def dst_email():
    return 'testimport@example.test'


@pytest.fixture
def src_login():
    return 'testimport@external.test'


@pytest.fixture
def uid():
    return 113 * 10 ** 13


@pytest.fixture
def dir_user(mocker, uid, dst_email):
    return mocker.Mock(uid=uid, email=dst_email)


@pytest.fixture
def password(mock_encryptor_iv):
    return Password.from_plain('password')


@pytest.fixture
def user_info(user, password, src_login):
    return UserInfo(
        login=user.login,
        password=password,
        src_login=src_login,
    )


@pytest.fixture
def yarm_collector_status(rands, randn):
    return YarmCollectorStatus(
        collected=randn(),
        errors=randn(),
        total=randn(),
        folders=[
            YarmCollectorFolderStatus(
                name=rands(),
                collected=randn(),
                errors=randn(),
                total=randn()
            ) for _ in range(randn(max=10))
        ]
    )


@pytest.fixture
def yarm_collector(rands, randbool, randitem, randn, yarm_collector_status):
    return YarmCollector(
        pop_id=rands(),
        server=rands(),
        port=randn(),
        login=rands(),
        ssl=randbool(),
        email=rands(),
        imap=randbool(),
        state=randitem(list(YarmCollectorState)),
        delete_msgs=randbool(),
        status=yarm_collector_status
    )
