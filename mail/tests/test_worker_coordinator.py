import copy
import json
from collections import namedtuple
from datetime import datetime
from unittest.mock import ANY, MagicMock, call

import pytest
from pytest import fixture
from tractor.models import (
    Task,
    TaskWorkerStatus,
)
from tractor.disk.models import (
    TaskType,
    UserMigration,
    UserMigrationStatus,
    UserStatistics,
)
from tractor.tests.fixtures.common import *
from tractor.error import *
from tractor_disk.workers.env import Env
from tractor_disk.workers.coordinator import processes_finished_list_task

TASK_ID = "test_task_id"


@fixture
def env():
    env = {
        "db": MagicMock(),
        "logger": MagicMock(),
    }
    Settings = namedtuple("settings", "sync")
    Sync = namedtuple("Sync", "worker_chunk_size")
    env["settings"] = Settings(sync=Sync(worker_chunk_size=100))
    return env


@fixture
def task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=datetime.now(),
        type=TaskType.LIST,
        input={},
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.SUCCESS,
        worker_ts=datetime.now(),
        worker_output={
            "files_count": 100,
            "quota": 999,
            "files_size": 993,
            "stid": "stid",
        },
    )


@fixture
def migration():
    return UserMigration(
        org_id=ORG_ID,
        domain=DOMAIN,
        login="",
        status=UserMigrationStatus.LISTING,
        list_task_id=TASK_ID,
        sync_task_ids=[],
        stats=UserStatistics(),
    )


@fixture
def cur():
    return MagicMock()


def test_should_fail_migration_when_task_cancelled(migration, task, env: Env, cur):
    expected = copy.deepcopy(migration)
    task.canceled = True
    processes_finished_list_task(migration, task, env, cur)

    expected.status = UserMigrationStatus.ERROR
    expected.error_reason = OPERATION_CANCELED
    expected.stats = UserStatistics(
        files_count=100,
        quota=999,
        files_size=993,
    )

    env["db"].mark_migration_finished.assert_called_once_with(
        org_id=migration.org_id,
        login=migration.login,
        status=expected.status.value,
        error_reason=expected.error_reason,
        stats=migration.stats,
        cur=cur,
    )


def test_should_fail_migration_when_task_failed(migration, task, env: Env, cur):
    expected = copy.deepcopy(migration)
    task.worker_status = TaskWorkerStatus.ERROR
    processes_finished_list_task(migration, task, env, cur)

    expected.status = UserMigrationStatus.ERROR
    expected.error_reason = LISTING_ERROR
    expected.stats = UserStatistics(
        files_count=100,
        quota=999,
        files_size=993,
    )
    migration.status = UserMigrationStatus.ERROR
    env["db"].mark_migration_finished.assert_called_once_with(
        org_id=migration.org_id,
        login=migration.login,
        status=expected.status.value,
        error_reason=expected.error_reason,
        stats=migration.stats,
        cur=cur,
    )


def test_should_forward_external_user_not_found_error(migration, task, env: Env, cur):
    task.worker_status = TaskWorkerStatus.ERROR
    task.worker_output = {"error": EXTERNAL_USER_NOT_FOUND}

    processes_finished_list_task(migration, task, env, cur)

    env["db"].mark_migration_finished.assert_called_once_with(
        org_id=migration.org_id,
        login=migration.login,
        status=UserMigrationStatus.ERROR.value,
        error_reason=EXTERNAL_USER_NOT_FOUND,
        stats=ANY,
        cur=ANY,
    )


@pytest.mark.parametrize("missing", ["stid", "files_count"])
def test_should_raise_in_case_of_incorrect_worker_output(migration, task, env: Env, missing, cur):
    with pytest.raises(KeyError):
        del task.worker_output[missing]
        processes_finished_list_task(migration, task, env, cur)


def test_should_create_task(migration, task, env: Env, cur):
    sync_task_id = 42
    expected = copy.deepcopy(migration)

    env["db"].create_task.return_value = sync_task_id

    processes_finished_list_task(migration, task, env, cur)

    expected.status = UserMigrationStatus.SYNCING
    expected.sync_task_ids = [sync_task_id]
    expected.stats = UserStatistics(
        files_count=100,
        quota=999,
        files_size=993,
    )

    env["db"].update_migration_to_sync_state.assert_called_once_with(
        org_id=migration.org_id,
        login=migration.login,
        sync_task_ids=migration.sync_task_ids,
        stats=migration.stats,
        cur=ANY,
    )

    inp = {"workers_count": 1, "stid": "stid", "worker_num": 0}
    inp = json.dumps(inp)
    env["db"].create_task.assert_called_once_with(
        type=TaskType.SYNC, org_id=task.org_id, domain=task.domain, worker_input=inp, cur=ANY
    )


def test_should_create_multiple_task(migration, task, env: Env, cur):
    sync_task_id = 42
    expected = copy.deepcopy(migration)
    task.worker_output["files_count"] = env["settings"].sync.worker_chunk_size * 2 - 1

    env["db"].create_task.side_effect = [sync_task_id, 2 * sync_task_id]

    processes_finished_list_task(migration, task, env, cur)

    expected.status = UserMigrationStatus.SYNCING
    expected.sync_task_ids = [sync_task_id, 2 * sync_task_id]
    expected.stats = UserStatistics(
        files_count=env["settings"].sync.worker_chunk_size * 2 - 1,
        quota=999,
        files_size=993,
    )

    env["db"].update_migration_to_sync_state.assert_called_once_with(
        org_id=migration.org_id,
        login=migration.login,
        sync_task_ids=migration.sync_task_ids,
        stats=migration.stats,
        cur=ANY,
    )

    workers_count = 2
    inp = {"workers_count": workers_count, "stid": "stid", "worker_num": 0}

    expected_calls = []
    for i in range(workers_count):
        inp["worker_num"] = i
        expected_calls.append(
            call(
                type=TaskType.SYNC,
                org_id=task.org_id,
                domain=task.domain,
                worker_input=json.dumps(inp),
                cur=ANY,
            )
        )
    env["db"].create_task.assert_has_calls(expected_calls)
