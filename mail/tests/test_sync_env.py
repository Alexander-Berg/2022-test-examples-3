import copy
from datetime import datetime
from unittest.mock import ANY, MagicMock

import pytest
from tractor.models import Task, TaskWorkerStatus
from tractor.disk.models import TaskType
from tractor.tests.fixtures.common import *
from tractor_disk.workers.env import Env, make_sync_task_env, trycatch

TASK_INPUT = {
    "stid": "mds",
    "source": "google",
    "user": {
        "uid": 1,
        "login": "arcadi",
        "email": "arcadi@yandex-team.ru",
    },
    "worker_num": 0,
    "workers_count": 1,
}


make_sync_task_env = make_sync_task_env.__wrapped__


@pytest.fixture
def env():
    env = {
        "db": MagicMock(),
        "mds": MagicMock(),
        "logger": MagicMock(),
    }
    return env


@pytest.fixture
def task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=datetime.now(),
        type=TaskType.LIST,
        input=copy.deepcopy(TASK_INPUT),
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.PENDING,
        worker_ts=datetime.now(),
        worker_output="",
    )


@pytest.mark.parametrize("missing", ["user", "source"])
def test_should_raise_if_something_missed_in_input(task, env: Env, missing):
    with pytest.raises(KeyError):
        del task.input[missing]
        make_sync_task_env(task, env)


@pytest.mark.parametrize("missing", ["uid", "login", "email"])
def test_should_raise_if_something_missed_in_user(task, env: Env, missing):
    with pytest.raises(KeyError):
        user = task.input["user"]
        del user[missing]
        make_sync_task_env(task, env)


@pytest.mark.parametrize("missing", ["db", "mds"])
def test_should_raise_if_something_missed_in_env(task, env: Env, missing):
    with pytest.raises(KeyError):
        del env[missing]
        make_sync_task_env(task, env)


@pytest.mark.parametrize("provider", ["mailru", "ramlber"])
def test_should_raise_if_wrong_provider(task, env: Env, provider):
    with pytest.raises(ValueError):
        task.input["provider"] = provider
        make_sync_task_env(task, env)


def test_trycatch_should_fail_sync_task_in_case_of_exception(task, env: Env):
    task.type = TaskType.SYNC
    trycatch(MagicMock(side_effect=Exception("")))(task, env)
    env["db"].fail_sync_task.assert_called_once_with(error=ANY, task_id=task.task_id, cur=ANY)


def test_trycatch_should_fail_list_task_in_case_of_exception(task, env: Env):
    task.type = TaskType.LIST
    trycatch(MagicMock(side_effect=Exception("")))(task, env)
    env["db"].fail_listing_task.assert_called_once_with(error=ANY, task_id=task.task_id, cur=ANY)
