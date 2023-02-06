from fixtures import *
from utils import *
from tractor.settings import TractorDBSettings
from tractor.db import BaseDatabase as Database
from tractor.disk.models import TaskType
from tractor.models import TaskWorkerStatus
from psycopg2.extensions import cursor
from datetime import timedelta


@pytest.fixture
def db():
    settings = TractorDBSettings(CONNINFO)
    db = Database(settings, "tractor_disk")
    return db


@pytest.fixture
def db_cur(db: Database):
    with db.make_connection() as conn:
        with conn.cursor() as cur:
            yield cur
            conn.rollback()


@pytest.fixture
def list_task(db: Database, db_cur: cursor):
    task_id = db.create_task(TaskType.LIST, ORG_ID, DOMAIN, "{}", db_cur)
    return _get_task(task_id, db_cur)


def test_set_external_secret(org_id: str, db: Database, db_cur: cursor):
    db.set_external_secret(org_id, DOMAIN, PROVIDER, ENCRYPTED_SECRET, db_cur)
    secrets = _get_all_secrets_ordered_by_provider(org_id, db_cur)
    assert secrets == [
        {
            "org_id": org_id,
            "domain": DOMAIN,
            "provider": PROVIDER.value,
            "encrypted_secret": MemoryviewComparableBytes(ENCRYPTED_SECRET),
        }
    ]


def test_set_external_secret_with_provider_collision(
    org_id: str, org_id_2: str, db: Database, db_cur: cursor
):
    db.set_external_secret(org_id, DOMAIN, PROVIDER, ENCRYPTED_SECRET, db_cur)
    db.set_external_secret(org_id_2, DOMAIN_2, PROVIDER, ENCRYPTED_SECRET_2, db_cur)
    secrets = _get_all_secrets_ordered_by_provider(org_id, db_cur)
    secrets_2 = _get_all_secrets_ordered_by_provider(org_id_2, db_cur)
    assert secrets == [
        {
            "org_id": org_id,
            "domain": DOMAIN,
            "provider": PROVIDER.value,
            "encrypted_secret": MemoryviewComparableBytes(ENCRYPTED_SECRET),
        }
    ]
    assert secrets_2 == [
        {
            "org_id": org_id_2,
            "domain": DOMAIN_2,
            "provider": PROVIDER.value,
            "encrypted_secret": MemoryviewComparableBytes(ENCRYPTED_SECRET_2),
        }
    ]


def test_set_external_secret_with_full_collision(org_id: str, db: Database, db_cur: cursor):
    db.set_external_secret(org_id, DOMAIN, PROVIDER, ENCRYPTED_SECRET, db_cur)
    db.set_external_secret(org_id, DOMAIN_2, PROVIDER, ENCRYPTED_SECRET, db_cur)
    secrets = _get_all_secrets_ordered_by_provider(org_id, db_cur)
    assert secrets == [
        {
            "org_id": org_id,
            "domain": DOMAIN_2,
            "provider": PROVIDER.value,
            "encrypted_secret": MemoryviewComparableBytes(ENCRYPTED_SECRET),
        }
    ]


def test_get_external_secret(org_id: str, db: Database, db_cur: cursor):
    db.set_external_secret(org_id, DOMAIN, PROVIDER, ENCRYPTED_SECRET, db_cur)
    encrypted_secret = db.get_external_secret(org_id, PROVIDER, db_cur)
    assert isinstance(encrypted_secret, bytes)
    assert encrypted_secret == ENCRYPTED_SECRET


def test_create_task(db: Database, db_cur: cursor):
    task_id = db.create_task(TaskType.LIST, ORG_ID, DOMAIN, "{}", db_cur)
    task = _get_task(task_id, db_cur)
    assert task is not None
    assert task["task_id"] == task_id
    assert task["org_id"] == ORG_ID
    assert task["domain"] == DOMAIN
    assert task["type"] == TaskType.LIST
    assert task["canceled"] == False
    assert task["input"] == {}
    assert task["worker_status"] == TaskWorkerStatus.PENDING.value
    assert task["worker_id"] == ""


def test_acquire_task(list_task, db: Database, db_cur: cursor):
    # Any task may be acquired, not only `list_task`.
    task = db.acquire_task(TaskType.LIST, timedelta(days=1), WORKER_ID, db_cur)
    assert task is not None
    assert task.worker_id == WORKER_ID


def test_get_task_by_id(list_task, db: Database, db_cur: cursor):
    task = db.get_task_by_task_id(list_task["task_id"], db_cur)
    assert task.task_id == list_task["task_id"]
    assert task.org_id == list_task["org_id"]
    assert task.domain == list_task["domain"]
    assert task.created_ts == list_task["created_ts"]
    assert task.type == list_task["type"]
    assert task.input == list_task["input"]
    assert task.canceled == list_task["canceled"]
    assert task.worker_id == list_task["worker_id"]
    assert task.worker_status.value == list_task["worker_status"]
    assert task.worker_ts == list_task["worker_ts"]
    assert task.worker_output == list_task["worker_output"]


def _get_all_secrets_ordered_by_provider(org_id: str, db_cur: cursor):
    return fetch_all(
        "SELECT * FROM tractor.external_secrets WHERE org_id = %(org_id)s ORDER BY provider",
        {"org_id": org_id},
        db_cur,
    )


def _get_task(task_id, db_cur: cursor):
    return fetch_one(
        "SELECT * FROM tractor_disk.tasks WHERE task_id = %(task_id)s",
        {"task_id": task_id},
        db_cur,
    )
