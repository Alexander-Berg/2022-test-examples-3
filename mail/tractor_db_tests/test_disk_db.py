from fixtures import *
from utils import *
from tractor.disk.db import Database
from tractor.disk.models import TaskType, UserMigrationStatus
from tractor.settings import TractorDBSettings
from psycopg2.extensions import cursor


@pytest.fixture
def db():
    settings = TractorDBSettings(CONNINFO)
    db = Database(settings)
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


@pytest.fixture
def migration(login, list_task, db: Database, db_cur: cursor):
    db.create_user_migration(ORG_ID, DOMAIN, login, list_task["task_id"], db_cur)
    return _get_migration(ORG_ID, login, db_cur)


def test_create_migration(login, list_task, db: Database, db_cur: cursor):
    db.create_user_migration(ORG_ID, DOMAIN, login, list_task["task_id"], db_cur)
    migration = _get_migration(ORG_ID, login, db_cur)
    assert migration is not None
    assert migration["org_id"] == ORG_ID
    assert migration["login"] == login
    assert migration["domain"] == DOMAIN
    assert migration["status"] == UserMigrationStatus.LISTING.value
    assert migration["list_task_id"] == list_task["task_id"]
    assert migration["sync_task_ids"] == []


def test_get_migration(migration, db: Database, db_cur: cursor):
    ret = db.get_user_migration(ORG_ID, migration["login"], db_cur)
    assert ret.org_id == migration["org_id"]
    assert ret.login == migration["login"]
    assert ret.domain == migration["domain"]
    assert ret.status.value == migration["status"]
    assert ret.list_task_id == migration["list_task_id"]
    assert ret.sync_task_ids == migration["sync_task_ids"]


def _get_task(task_id, db_cur: cursor):
    return fetch_one(
        "SELECT * FROM tractor_disk.tasks WHERE task_id = %(task_id)s",
        {"task_id": task_id},
        db_cur,
    )


def _get_migration(org_id, login, db_cur: cursor):
    return fetch_one(
        "SELECT * FROM tractor_disk.user_migrations WHERE org_id = %(org_id)s AND login = %(login)s",
        {
            "org_id": org_id,
            "login": login,
        },
        db_cur,
    )
