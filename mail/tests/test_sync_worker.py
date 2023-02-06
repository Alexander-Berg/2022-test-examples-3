from decimal import DivisionByZero
import json
from datetime import datetime, timedelta
from unittest.mock import ANY, MagicMock, call

import pytest
from tractor.tests.fixtures.common import *
from tractor.models import Task, TaskWorkerStatus
from tractor.disk.models import TaskType
from pytest import fixture
from tractor_disk.settings import TaskingSettings
from tractor_disk.workers.env import Env
from tractor_disk.workers.sync_worker import run_task, trycatch, acquire_task
from tractor_disk.workers.list_worker import dump_listing_args
from tractor_disk.disk_error import DiskTemporaryError, DiskPermanentError, exception_to_category


FILES = [
    {"id": "id1", "path": "path1", "mimeType": "mime1"},
    {"id": "id2", "path": "path2", "mimeType": "mime2"},
    {"id": "id2", "path": "path2", "mimeType": "mime2"},
]

TASK_INPUT = {
    "stid": "mds",
    "worker_num": 0,
    "workers_count": 1,
}


@fixture
def env():
    env = {
        "worker_id": "sas7-0123-4.56789abcdefghijk.sas.yp-c.yandex.net/12345",
        "db": MagicMock(),
        "mds": MagicMock(),
        "logger": MagicMock(),
        "sync_op": MagicMock(),
        "settings": MagicMock(),
    }
    return env


@fixture
def task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=datetime.now(),
        type=TaskType.SYNC,
        input=TASK_INPUT.copy(),
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.PENDING,
        worker_ts=datetime.now(),
        worker_output="",
    )


def expected_progress(synced_count=0, exists_count=0, error_count={}, total_count=0):
    err_cnt = 0
    for _, val in error_count.items():
        err_cnt += val
    return json.dumps(
        {
            "synced": synced_count,
            "exists": exists_count,
            "error": err_cnt,
            "error_category": error_count,
            "processed": synced_count + exists_count + err_cnt,
            "total": total_count,
        }
    )


def test_acquire_task(env: Env, task):
    env["settings"].tasking = TaskingSettings()
    env["db"].acquire_task = MagicMock(return_value=task)
    actual_task = acquire_task(env)
    env["db"].acquire_task.assert_called_with(
        type=TaskType.SYNC,
        expiry_timeout=timedelta(seconds=env["settings"].tasking.expiry_timeout_in_seconds),
        worker_id=env["worker_id"],
        cur=ANY,
    )
    assert actual_task is task


def test_should_not_do_anything_with_cancelled_task(task):
    task.canceled = True
    run_task(task=task, env=MagicMock(side_effect=Exception("")))


def test_should_not_do_anything_with_task_in_noninit_status(task):
    task.worker_status = TaskWorkerStatus.SUCCESS
    run_task(task=task, env=MagicMock(side_effect=Exception("")))

    task.worker_status = TaskWorkerStatus.ERROR
    run_task(task=task, env=MagicMock(side_effect=Exception("")))


def test_should_fail_if_mds_fails(task, env: Env):
    env["mds"].download.side_effect = Exception("mds fail")

    run_task(task, env)

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])
    env["db"].fail_sync_task.assert_called_once_with(error=ANY, task_id=task.task_id, cur=ANY)
    env["sync_op"].assert_not_called()


def test_should_fail_if_mds_resp_is_incorrect(task, env: Env):
    mds_chunk = {"incorrect": "response"}
    env["mds"].download.return_value = json.dumps(mds_chunk)

    run_task(task, env)

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])
    env["db"].fail_sync_task.assert_called_once()
    env["db"].finish_sync_task.assert_not_called()
    env["sync_op"].assert_not_called()


def test_should_finish_correctly_if_empty_list(task, env: Env):
    env["mds"].download.return_value = dump_listing_args([], [], [])

    run_task(task, env)

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])

    progress = expected_progress(synced_count=0)
    env["db"].finish_sync_task.assert_called_once_with(
        output=progress, task_id=task.task_id, cur=ANY
    )

    env["db"].fail_sync_task.assert_not_called()
    env["sync_op"].assert_not_called()


def test_should_correctly_sync(task, env: Env):
    env["mds"].download.return_value = dump_listing_args(FILES, [], [])
    env["sync_op"].return_value = True

    run_task(task, env)

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])

    progress = expected_progress(synced_count=len(FILES), total_count=len(FILES))
    env["db"].finish_sync_task.assert_called_once_with(
        output=progress, task_id=task.task_id, cur=ANY
    )

    env["db"].fail_sync_task.assert_not_called()

    assert env["logger"].info.call_count > len(FILES)


@pytest.mark.parametrize("worker_num", [0, 1, 2])
def test_multiple_workers(task, env: Env, worker_num):
    workers_count = 3
    files = []
    for _ in range(workers_count):
        files.extend(FILES)

    env["mds"].download.return_value = dump_listing_args(files, [], [])
    env["sync_op"].return_value = True

    params = TASK_INPUT.copy()
    params["worker_num"], params["workers_count"] = worker_num, workers_count
    task.input = params
    run_task(task, env)

    synced = len(files) // workers_count

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])

    file = FILES[(worker_num) % len(FILES)]
    env["sync_op"].assert_has_calls([call(file) for _ in range(synced)])

    progress = expected_progress(synced_count=synced, total_count=synced)
    env["db"].finish_sync_task.assert_called_once_with(
        output=progress, task_id=task.task_id, cur=ANY
    )

    env["db"].fail_sync_task.assert_not_called()

    assert env["logger"].info.call_count > synced


def test_should_handle_existing_files(task, env: Env):
    env["mds"].download.return_value = dump_listing_args(FILES, [], [])
    env["sync_op"].side_effect = [True, False, True, True]

    run_task(task, env)

    progress = expected_progress(
        synced_count=len(FILES) - 1, exists_count=1, total_count=len(FILES)
    )
    env["db"].finish_sync_task.assert_called_once_with(
        output=progress, task_id=task.task_id, cur=ANY
    )

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])

    env["sync_op"].assert_has_calls([call(row) for row in FILES])

    env["db"].fail_sync_task.assert_not_called()

    assert env["logger"].info.call_count > len(FILES)


@pytest.mark.parametrize(
    "error",
    [
        Exception("sync failed"),
        RuntimeError("sync failed"),
        NotImplementedError("sync failed"),
        DivisionByZero("sync failed"),
    ],
)
def test_should_handle_sync_op_internal_errors(task, env: Env, error):
    env["mds"].download.return_value = dump_listing_args(FILES, [], [])
    env["sync_op"].side_effect = [True, error, True, True]
    _, err_category = exception_to_category(error)

    run_task(task, env)

    env["db"].fail_sync_task.assert_not_called()

    progress = expected_progress(
        synced_count=len(FILES) - 1, error_count={err_category: 1}, total_count=len(FILES)
    )
    env["db"].finish_sync_task.assert_called_once_with(
        output=progress, task_id=task.task_id, cur=ANY
    )

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])

    env["sync_op"].assert_has_calls([call(row) for row in FILES])

    assert env["logger"].info.call_count > len(FILES)


@pytest.mark.parametrize(
    "error, source",
    [
        (DiskTemporaryError, "SouRce"),
        (DiskTemporaryError, "dst"),
        (DiskPermanentError, "dst"),
        (DiskPermanentError, "Yandex"),
        (DiskPermanentError, "Google"),
    ],
)
def test_should_handle_sync_op_disk_errors(task, env: Env, error, source):
    files = FILES
    env["mds"].download.return_value = dump_listing_args(files, [], [])

    disk_err, err_category = exception_to_category(error(message="", source=source))
    error_count = {err_category: 2}

    env["sync_op"].side_effect = [True, disk_err, disk_err]

    run_task(task, env)

    env["db"].fail_sync_task.assert_not_called()

    progress = expected_progress(synced_count=1, error_count=error_count, total_count=len(files))
    env["db"].finish_sync_task.assert_called_once_with(
        output=progress, task_id=task.task_id, cur=ANY
    )

    env["mds"].download.assert_called_once_with(filepath=TASK_INPUT["stid"])

    env["sync_op"].assert_has_calls([call(row) for row in files])


def test_trycatch_should_fail_task_in_case_of_exception(task, env: Env):
    trycatch(MagicMock(side_effect=Exception("")))(task, env)

    env["db"].fail_sync_task.assert_called_once_with(error=ANY, task_id=task.task_id, cur=ANY)
