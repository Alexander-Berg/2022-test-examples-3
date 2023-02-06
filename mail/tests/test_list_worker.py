import json
from datetime import datetime, timedelta
from unittest.mock import ANY, MagicMock

from pytest import fixture
from tractor.models import Task, TaskWorkerStatus
from tractor.disk.models import TaskType
from tractor.error import NOT_ENOUGH_QUOTA, EXTERNAL_USER_NOT_FOUND
from tractor.tests.fixtures.common import *
from tractor_disk.common import NULL_STR
from tractor_disk.settings import TaskingSettings
from tractor_disk.disk_error import ExternalUserNotFound
from tractor_disk.source_drive import SourceDrive, get_file_by_source
from tractor_disk.workers.env import Env
from tractor_disk.workers.list_worker import dump_listing_args, acquire_task, run_task, trycatch


@fixture
def env():
    env = {
        "worker_id": "sas7-0123-4.56789abcdefghijk.sas.yp-c.yandex.net/12345",
        "settings": MagicMock(),
        "db": MagicMock(),
        "disk_pair": MagicMock(),
        "mds": MagicMock(),
        "mapping": MagicMock(),
        "logger": MagicMock(),
    }
    return env


def raw_task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=datetime.now(),
        type=TaskType.LIST,
        input="",  # TODO
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.PENDING,
        worker_ts=datetime.now(),
        worker_output="",
    )


@fixture
def task():
    return raw_task()


def test_acquire_task(env: Env):
    test_task = raw_task()
    env["settings"].tasking = TaskingSettings()
    env["db"].acquire_task = MagicMock(return_value=test_task)
    actual_task = acquire_task(env)
    env["db"].acquire_task.assert_called_with(
        type=TaskType.LIST,
        expiry_timeout=timedelta(seconds=env["settings"].tasking.expiry_timeout_in_seconds),
        worker_id=env["worker_id"],
        cur=ANY,
    )
    assert actual_task is test_task


def test_should_not_do_anything_with_cancelled_task(task, env: Env):
    task.canceled = True
    env["db"].make_connection.retun_value = MagicMock()
    env["db"].set_error_for_cancelled_task = MagicMock()

    run_task(task=task, env=env)

    env["db"].set_error_for_cancelled_task.assert_called_once_with(task_id=task.task_id, cur=ANY)


def test_should_not_do_anything_with_task_in_noninit_status(task):
    task.worker_status = TaskWorkerStatus.SUCCESS
    run_task(task=task, env=MagicMock(side_effect=Exception("")))

    task.worker_status = TaskWorkerStatus.ERROR
    run_task(task=task, env=MagicMock(side_effect=Exception("")))


def test_should_write_mappings_to_mds(task, env: Env):
    files = [
        {"name": "file1", "id": "id1", "size": "588", "ownedByMe": True},
        {"name": "file2", "id": "id2", "size": "600", "ownedByMe": True},
    ]
    ya_disk_quota = {"total_space": 10000}

    id_to_path = {"id1": "path1", "id2": "path2"}
    multiparent = ["file2"]
    saturated = [
        {
            "id": "id1",
            "size": "588",
            "path": "path1",
            "mimeType": NULL_STR,
            "ownedByMe": True,
        },
        {
            "id": "id2",
            "size": "600",
            "path": "path2",
            "mimeType": NULL_STR,
            "ownedByMe": True,
        },
    ]

    env["file_cls"] = lambda x: get_file_by_source(SourceDrive.GOOGLE)(x)
    env["disk_pair"].src_disk.get_files.return_value = files
    env["disk_pair"].dst_disk.quote.return_value = ya_disk_quota
    env["mapping"].return_value = MagicMock(return_value=(id_to_path, multiparent))
    env["mds"].upload = MagicMock(return_value="stid")
    env["db"].finish_listing_task = MagicMock()
    env["db"].make_connection.retun_value = MagicMock()

    run_task(task, env)
    files_size = sum(int(file["size"]) for file in saturated)

    output = json.dumps(
        {
            "stid": "stid",
            "files_count": len(saturated),
            "files_size": files_size,
            "quota": ya_disk_quota["total_space"],
        }
    )
    env["db"].finish_task.assert_called_once_with(output=output, task_id=task.task_id, cur=ANY)

    data = dump_listing_args(files=saturated, multiple_parent_files=multiparent, skipped_files=[])
    env["mds"].upload.assert_called_once_with(filename="listing", data=data)
    env["mapping"].assert_called_once_with(files=files)


def test_should_fail_task_in_case_of_quota_not_enough(task, env: Env):
    files = [
        {"name": "file1", "id": "id1", "size": "588", "ownedByMe": True},
        {"name": "file2", "id": "id2", "size": "600", "ownedByMe": True},
    ]
    id_to_path = {"id1": "path1", "id2": "path2"}
    multiparent = ["file2"]
    ya_disk_quota = {"total_space": 1000}

    env["file_cls"] = lambda x: get_file_by_source(SourceDrive.GOOGLE)(x)
    env["disk_pair"].src_disk.get_files.return_value = files
    env["disk_pair"].dst_disk.quote.return_value = ya_disk_quota
    env["mapping"].return_value = MagicMock(return_value=(id_to_path, multiparent))
    env["mds"].upload = MagicMock(return_value="stid")
    env["db"].make_connection.retun_value = MagicMock()

    run_task(task, env)

    env["db"].fail_listing_task.assert_called_once_with(
        error=NOT_ENOUGH_QUOTA,
        task_id=task.task_id,
        cur=ANY,
        stid="stid",
        files_count=len(files),
        files_size=ANY,
        quota=ya_disk_quota["total_space"],
    )


def test_trycatch_should_fail_task_in_case_of_exception(task, env: Env):
    env["db"].make_connection.retun_value = MagicMock()
    env["db"].fail_listing_task = MagicMock()

    trycatch(MagicMock(side_effect=Exception("")))(task, env)

    env["db"].fail_listing_task.assert_called_once_with(error=ANY, task_id=task.task_id, cur=ANY)


def test_trycatch_should_set_external_user_not_found_error(task, env: Env):
    env["db"].make_connection.retun_value = MagicMock()
    env["db"].fail_listing_task = MagicMock()

    trycatch(MagicMock(side_effect=ExternalUserNotFound("")))(task, env)

    env["db"].fail_listing_task.assert_called_once_with(
        error=EXTERNAL_USER_NOT_FOUND, task_id=task.task_id, cur=ANY
    )
