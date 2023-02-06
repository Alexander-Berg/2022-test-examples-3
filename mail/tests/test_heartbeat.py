from typing import Final
from contextlib import nullcontext
from datetime import timedelta, datetime
import threading
from unittest.mock import NonCallableMock, Mock, seal, patch, ANY

import pytest

from tractor.models import TaskWorkerStatus, Task
from tractor.disk.models import TaskType
from tractor.tests.fixtures.common import *
from tractor_disk.settings import TaskingSettings
from tractor_disk.workers.env import Env
from tractor_disk.workers.heartbeat import heartbeat


REST_PERIOD_IN_SECONDS: Final = 1
LOCAL_WORKER_ID: Final = "sas0-0123-4.abcdefghijklmnop.sas.yp-c.yandex.net/12345"

REFRESHED_TASK: Final = Task(
    task_id=TASK_ID,
    org_id=ORG_ID,
    domain=DOMAIN,
    created_ts=datetime.now(),
    type=TaskType.LIST,
    input={},
    canceled=False,
    worker_id=LOCAL_WORKER_ID,
    worker_status=TaskWorkerStatus.PENDING,
    worker_ts=datetime.now() + timedelta(seconds=REST_PERIOD_IN_SECONDS),
    worker_output={},
)


@pytest.fixture
def db_mock():
    connection = NonCallableMock()
    connection.cursor = lambda: nullcontext()
    connection.close = Mock()
    # `connection` is not sealed as to allow setting `autocommit` on it.
    db = NonCallableMock()
    db.make_connection = Mock(return_value=connection)
    db.refresh_task = Mock(return_value=REFRESHED_TASK)
    seal(db)
    return db


@pytest.fixture
def env(db_mock):
    env = {
        "worker_id": LOCAL_WORKER_ID,
        "settings": Mock(),
        "db": db_mock,
    }
    env["settings"].tasking = TaskingSettings(
        heartbeat_rest_period_in_seconds=REST_PERIOD_IN_SECONDS
    )
    return env


pytestmark: Final = pytest.mark.filterwarnings("error")


def test_rest_period_excess_raises_value_error():
    with pytest.raises(ValueError):
        TaskingSettings(expiry_timeout_in_seconds=100, heartbeat_rest_period_in_seconds=100)


def test_rest_period_no_excess_raises_nothing():
    with nullcontext() as raises_nothing:
        TaskingSettings(expiry_timeout_in_seconds=100, heartbeat_rest_period_in_seconds=99)


def test_tied_to_a_thread(env: Env):
    initial_thread_count = threading.active_count()
    hb = heartbeat(env, TASK_ID)
    assert threading.active_count() == initial_thread_count
    with hb:
        assert threading.active_count() == initial_thread_count + 1
    assert threading.active_count() == initial_thread_count


@pytest.mark.parametrize("period", [0, 1, 2])
def test_wait_timeout_is_correct(env: Env, period: int):
    env["settings"].tasking = TaskingSettings(heartbeat_rest_period_in_seconds=period)
    event_mock = NonCallableMock()
    event_mock.wait = Mock(return_value=True)
    with patch("tractor_disk.workers.heartbeat.Event", lambda: event_mock):
        with heartbeat(env, TASK_ID):
            pass
    event_mock.wait.assert_called_with(float(period))


def test_database_usage(env: Env):
    first_period_ended = threading.Event()

    def stop_requested_wait():
        yield False
        first_period_ended.set()
        yield True

    stop_requested_stub = NonCallableMock()
    stop_requested_stub.wait = Mock(side_effect=stop_requested_wait())

    database_mock = env["db"]

    hb = heartbeat(env, TASK_ID)
    with patch("tractor_disk.workers.heartbeat.Event", lambda: stop_requested_stub):
        database_mock.make_connection.assert_not_called()
        with hb:
            database_mock.make_connection.assert_called_once_with()
            connection_mock = database_mock.make_connection.return_value
            first_period_ended.wait()
            database_mock.refresh_task.assert_called_once_with(
                task_id=TASK_ID, worker_id=env["worker_id"], cur=ANY
            )
            connection_mock.close.assert_not_called()
        connection_mock.close.assert_called_once_with()
