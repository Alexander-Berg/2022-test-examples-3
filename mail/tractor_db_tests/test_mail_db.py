import json
from typing import Any, Final, List, Dict
from datetime import timedelta, datetime, timezone
from psycopg2.extensions import cursor
from tractor.models import TaskWorkerStatus
from tractor.settings import TractorDBSettings
from tractor.mail.models import (
    TaskType,
    MessagesCollectionStats,
    UserMigrationStatus,
    UserMigration,
    UserMigrationExportInfo,
    UserMigrationInfo,
)
from tractor.mail.db import Database
from utils import *
from fixtures import *


_INBOX_STATS_JSON: Final = dict(
    src_mailbox_messages_count=100,
    collected_messages_count=50,
    failed_to_collect_messages_count=0,
)
_EXPECTED_INBOX_ONLY_STATS: Final = MessagesCollectionStats(
    source_count=100,
    collected_count=50,
    failed_count=0,
)
_SENT_STATS_JSON: Final = dict(
    src_mailbox_messages_count=100,
    collected_messages_count=50,
    failed_to_collect_messages_count=3,
)
_EXPECTED_TOTAL_STATS: Final = MessagesCollectionStats(
    source_count=200,
    collected_count=100,
    failed_count=3,
)


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
def prepare_task(org_id, db: Database, db_cur: cursor):
    task_id = db.create_task(TaskType.PREPARE, org_id, DOMAIN, "{}", db_cur)
    return _get_task(task_id, db_cur)


@pytest.fixture
def migration(org_id, login, prepare_task, db: Database, db_cur: cursor):
    db.create_user_migration(org_id, DOMAIN, login, prepare_task["task_id"], db_cur)
    return _get_migration(org_id, login, db_cur)


@pytest.fixture
def stopping_migration(migration, db: Database, db_cur: cursor):
    _update_migration_status(
        migration["org_id"],
        migration["login"],
        UserMigrationStatus.STOPPING.value,
        db_cur,
    )
    return _get_migration(migration["org_id"], migration["login"], db_cur)


@pytest.fixture(
    params=[
        ({}, MessagesCollectionStats()),
        ({"inbox": _INBOX_STATS_JSON}, _EXPECTED_INBOX_ONLY_STATS),
        ({"inbox": _INBOX_STATS_JSON, "sent": _SENT_STATS_JSON}, _EXPECTED_TOTAL_STATS),
    ],
    ids=lambda param: len(param[0]),
)
def export_info_of_migration_with_n_folders(
    org_id: str,
    login: str,
    migration: Dict[str, Any],
    request: pytest.FixtureRequest,
    db: Database,
    db_cur: cursor,
):
    assert migration["stats"] == {}, migration
    param: tuple = get_fixture_param(request)
    folders_json: Dict[str, Dict[str, int]] = param[0]
    expected_stats: MessagesCollectionStats = param[1]
    db.update_stats(org_id, login, {"folders": folders_json}, db_cur)
    return UserMigrationExportInfo(
        login=migration["login"],
        status=UserMigrationStatus(migration["status"]),
        error_reason=migration["error_reason"],
        mailbox_messages_collection_stats=expected_stats,
    )


@pytest.fixture
def stop_task(org_id, db: Database, db_cur: cursor):
    task_id = db.create_task(TaskType.STOP, org_id, DOMAIN, "{}", db_cur)
    return _get_task(task_id, db_cur)


@pytest.fixture
def migration_info_1(org_id: str, login: str, db: Database, db_cur: cursor):
    task_id = db.create_task(TaskType.PREPARE, org_id, DOMAIN, "{}", db_cur)
    return _insert_user_migration(db, db_cur, org_id, login, task_id)


@pytest.fixture
def migration_info_2(org_id: str, login_2: str, db: Database, db_cur: cursor):
    task_id = db.create_task(TaskType.PREPARE, org_id, DOMAIN, "{}", db_cur)
    return _insert_user_migration(db, db_cur, org_id, login_2, task_id)


def test_create_migration(login, prepare_task, db: Database, db_cur: cursor):
    org_id = prepare_task["org_id"]
    db.create_user_migration(org_id, DOMAIN, login, prepare_task["task_id"], db_cur)
    migration = _get_migration(org_id, login, db_cur)
    assert migration is not None
    assert migration["org_id"] == org_id
    assert migration["login"] == login
    assert migration["domain"] == DOMAIN
    assert migration["status"] == UserMigrationStatus.PREPARING.value
    assert migration["prepare_task_id"] == prepare_task["task_id"]


def test_get_migration(migration, db: Database, db_cur: cursor):
    ret = db.get_user_migration(
        migration["org_id"], migration["login"], db_cur
    )
    assert ret.org_id == migration["org_id"]
    assert ret.login == migration["login"]
    assert ret.domain == migration["domain"]
    assert ret.status.value == migration["status"]
    assert ret.error_reason == migration["error_reason"]
    assert ret.prepare_task_id == migration["prepare_task_id"]
    assert ret.stop_task_id == migration["stop_task_id"]


def test_reset_migration(migration, prepare_task, db: Database, db_cur: cursor):
    new_prepare_task_id = prepare_task["task_id"]
    db.reset_user_migration(
        migration["org_id"],
        migration["login"],
        migration["domain"],
        new_prepare_task_id,
        db_cur,
    )
    new_migration: UserMigration = db.get_user_migration(
        migration["org_id"], migration["login"], db_cur
    )
    assert new_migration.status == UserMigrationStatus.PREPARING
    assert new_migration.error_reason == ""
    assert new_migration.prepare_task_id == new_prepare_task_id
    assert new_migration.stats == {}
    assert new_migration.stop_task_id == None


def test_get_user_migration_statuses_with_no_users(db: Database, db_cur: cursor):
    statuses = db.get_user_migration_statuses(NONEXISTENT_ORG_ID, db_cur)
    assert statuses == []


def test_get_user_migration_statuses_with_one_user(
    migration_info_1: UserMigrationInfo, org_id: str, db: Database, db_cur: cursor
):
    statuses = db.get_user_migration_statuses(org_id, db_cur)
    assert statuses == [migration_info_1]


def test_get_user_migration_statuses_with_two_users(
    migration_info_1: UserMigrationInfo,
    migration_info_2: UserMigrationInfo,
    org_id: str,
    db: Database,
    db_cur: cursor,
):
    statuses = db.get_user_migration_statuses(org_id, db_cur)
    statuses.sort(key=_login_of_migration_info)
    assert statuses == [migration_info_1, migration_info_2]


def test_export_user_migrations_with_no_users(org_id: str, db: Database, db_cur: cursor):
    stats = db.export_user_migrations(org_id, db_cur)
    assert stats == []


def test_export_user_migrations_without_folders(
    org_id: str, migration: Dict[str, Any], db: Database, db_cur: cursor
):
    stats = db.export_user_migrations(org_id, db_cur)
    assert stats == [
        UserMigrationExportInfo(
            login=migration["login"],
            status=UserMigrationStatus(migration["status"]),
            error_reason=migration["error_reason"],
            mailbox_messages_collection_stats=None,
        )
    ]


def test_export_user_migrations_with_n_folders(
    org_id: str,
    export_info_of_migration_with_n_folders: UserMigrationExportInfo,
    db: Database,
    db_cur: cursor,
):
    stats: List[UserMigrationExportInfo] = db.export_user_migrations(org_id, db_cur)
    assert stats == [export_info_of_migration_with_n_folders]


def test_acquire_and_stop_migrations_should_save_old_status(
    migration, db: Database, db_cur: cursor
):
    acquired_migrations = db.acquire_and_stop_migrations(migration["org_id"], db_cur)
    assert migration["status"] == acquired_migrations[0]["previous_status"]


def test_acquire_and_stop_migrations_should_take_migration(migration, db: Database, db_cur: cursor):
    acquired_migrations = db.acquire_and_stop_migrations(migration["org_id"], db_cur)
    res = _get_migration(migration["org_id"], migration["login"], db_cur)
    assert res["login"] == acquired_migrations[0]["login"]
    assert res["prepare_task_id"] == acquired_migrations[0]["prepare_task_id"]


def test_acquire_and_stop_migrations_should_stop_migration_in_preparing_status(
    migration, db: Database, db_cur: cursor
):
    db.acquire_and_stop_migrations(migration["org_id"], db_cur)
    res = _get_migration(migration["org_id"], migration["login"], db_cur)
    assert res["status"] == "stopping"


def test_acquire_and_stop_migrations_should_stop_migration_in_initial_sync_status(
    migration, db: Database, db_cur: cursor
):
    _update_migration_status(migration["org_id"], migration["login"], "initial_sync", db_cur)
    db.acquire_and_stop_migrations(migration["org_id"], db_cur)
    res = _get_migration(migration["org_id"], migration["login"], db_cur)
    assert res["status"] == "stopping"


def test_acquire_and_stop_migrations_should_stop_migration_in_sync_newest_status(
    migration, db: Database, db_cur: cursor
):
    _update_migration_status(migration["org_id"], migration["login"], "sync_newest", db_cur)
    db.acquire_and_stop_migrations(migration["org_id"], db_cur)
    res = _get_migration(migration["org_id"], migration["login"], db_cur)
    assert res["status"] == "stopping"


def test_cancel_prepare_task_for_migration(migration, db: Database, db_cur: cursor):
    db.cancel_task(migration["prepare_task_id"], db_cur)
    task = _get_task(migration["prepare_task_id"], db_cur)
    assert task["canceled"] == True


def test_attach_stop_task_to_migration(migration, stop_task, db: Database, db_cur: cursor):
    db.attach_stop_task_to_migration(
        stop_task["task_id"], migration["org_id"], migration["login"], db_cur
    )
    res = _get_migration(migration["org_id"], migration["login"], db_cur)
    assert res["stop_task_id"] == stop_task["task_id"]


def test_acquire_migration_for_stats_update(migration, db: Database, db_cur: cursor):
    _update_migration_status(migration["org_id"], migration["login"], "sync_newest", db_cur)
    # Any migration may be acquired, not only `migration`.
    acquired_migration = db.acquire_migration_for_stats_update(timedelta(days=1), db_cur)
    stats_touch_ts = _get_migration(
        acquired_migration.org_id,
        acquired_migration.login,
        db_cur,
    )["stats_touch_ts"]
    assert datetime.now(timezone.utc) - stats_touch_ts < timedelta(minutes=1)


def test_update_stats(migration, db: Database, db_cur: cursor):
    new_stats = {
        "folders": {
            "test_folder": {
                "src_mailbox_messages_count": 3,
                "collected_messages_count": 2,
                "failed_to_collect_messages_count": 1,
            }
        }
    }
    db.update_stats(migration["org_id"], migration["login"], new_stats, db_cur)
    assert _get_migration(migration["org_id"], migration["login"], db_cur)["stats"] == new_stats


def test_mark_migration_finished(migration, db: Database, db_cur: cursor):
    db.mark_migration_finished(
        migration["org_id"], migration["login"], UserMigrationStatus.STOPPED, "", db_cur
    )
    assert (
        _get_migration(migration["org_id"], migration["login"], db_cur)["status"]
        == UserMigrationStatus.STOPPED.value
    )


def test_acquire_migration_in_stopping_status(stopping_migration, stop_task, db: Database, db_cur: cursor):
    db.attach_stop_task_to_migration(
        stop_task["task_id"], stopping_migration["org_id"], stopping_migration["login"], db_cur
    )
    _update_task(
        stop_task["task_id"],
        TaskWorkerStatus.SUCCESS.value,
        json.dumps({}),
        db_cur,
    )
    ret = db.acquire_migration_in_stopping_status(db_cur)
    assert ret is not None
    assert ret.org_id == stopping_migration["org_id"]
    assert ret.login == stopping_migration["login"]
    assert ret.domain == stopping_migration["domain"]
    assert ret.status.value == stopping_migration["status"]
    assert ret.error_reason == stopping_migration["error_reason"]
    assert ret.prepare_task_id == stopping_migration["prepare_task_id"]
    assert ret.stop_task_id == stop_task["task_id"]


def test_acquire_migration_in_preparing_status(migration, db: Database, db_cur: cursor):
    _update_task(
        migration["prepare_task_id"],
        TaskWorkerStatus.SUCCESS.value,
        json.dumps({}),
        db_cur,
    )
    ret = db.acquire_migration_in_preparing_status(db_cur)
    assert ret is not None
    assert ret.org_id == migration["org_id"]
    assert ret.login == migration["login"]
    assert ret.domain == migration["domain"]
    assert ret.status.value == migration["status"]
    assert ret.error_reason == migration["error_reason"]
    assert ret.prepare_task_id == migration["prepare_task_id"]
    assert ret.stop_task_id == migration["stop_task_id"]


def _login_of_migration_info(info: UserMigrationInfo):
    return info.login


def _insert_user_migration(
    db: Database,
    db_cur: cursor,
    org_id: str,
    login: str,
    prepare_task_id: int,
):
    db.create_user_migration(org_id, DOMAIN, login, prepare_task_id, db_cur)
    migration = _get_migration(org_id, login, db_cur)
    if migration is None:
        raise RuntimeError("Failed to retrieve migration that was just created")
    return UserMigrationInfo(
        login=migration["login"],
        status=UserMigrationStatus(migration["status"]),
        error_reason=migration["error_reason"],
    )


def _get_task(task_id, db_cur: cursor):
    return fetch_one(
        "SELECT * FROM tractor_mail.tasks WHERE task_id = %(task_id)s",
        {"task_id": task_id},
        db_cur,
    )


def _get_migration(org_id, login, db_cur: cursor):
    return fetch_one(
        "SELECT * FROM tractor_mail.user_migrations WHERE org_id = %(org_id)s AND login = %(login)s",
        {
            "org_id": org_id,
            "login": login,
        },
        db_cur,
    )


def _update_migration_status(org_id, login, new_status, db_cur: cursor):
    db_cur.execute(
        """
    UPDATE tractor_mail.user_migrations
    SET status = %(new_status)s
    WHERE
    org_id = %(org_id)s
    AND
    login = %(login)s
    """,
        {"org_id": org_id, "login": login, "new_status": new_status},
    )


def _update_task(task_id, worker_status, input, db_cur: cursor):
    db_cur.execute(
        """
            UPDATE tractor_mail.tasks
            SET
                input = %(input)s,
                worker_status = %(worker_status)s
            WHERE
                task_id = %(task_id)s
        """,
        {
            "task_id": task_id,
            "input": input,
            "worker_status": worker_status,
        },
    )
