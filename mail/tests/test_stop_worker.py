import traceback
from pytest import fixture
from unittest.mock import ANY, MagicMock
from tractor.mail.models import TaskType
from tractor.models import Task, TaskWorkerStatus
from tractor.tests.fixtures.common import *
from tractor_mail.impl.stop_worker import run_task

USER_UID = "test_user_uid"
USER_SUID = "test_user_suid"
ADMIN_UID = "test_admin_uid"
POPID = "test_popid"

STOP_TASK_INPUT = {"prepare_task_id": TASK_ID}

PREPARE_TASK_OUTPUT = {
    "uid": USER_UID,
    "suid": USER_SUID,
    "popid": POPID,
}


def _throw_test_exception(*args, **kwargs):
    raise RuntimeError("test exception")


@fixture
def prepare_task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=ANY,
        type=TaskType.PREPARE,
        input=ANY,
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.PENDING,
        worker_ts=ANY,
        worker_output=PREPARE_TASK_OUTPUT,
    )


@fixture
def failed_prepare_task(prepare_task):
    prepare_task.worker_status = TaskWorkerStatus.ERROR
    return prepare_task

@fixture
def pending_prepare_task(prepare_task):
    prepare_task.worker_status = TaskWorkerStatus.PENDING
    return prepare_task


@fixture
def finished_prepare_task(prepare_task):
    prepare_task.worker_status = TaskWorkerStatus.SUCCESS
    return prepare_task


@fixture
def stop_task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=ANY,
        type=TaskType.STOP,
        input=STOP_TASK_INPUT,
        canceled=False,
        worker_id=ANY,
        worker_status=TaskWorkerStatus.PENDING,
        worker_ts=ANY,
        worker_output="",
    )


@fixture
def env(prepare_task):
    env = {
        "settings": MagicMock(),
        "db": MagicMock(),
        "logger": MagicMock(),
        "collectors": MagicMock(),
    }
    env["db"].get_task_by_task_id.return_value = prepare_task
    env["collectors"].create.return_value = POPID

    yield env

    if env["logger"].exception.called:
        e = env["logger"].exception.call_args.kwargs["exception"]
        traceback.print_tb(e.__traceback__)


def test_should_delete_collector_if_polling_prepare_task_finished_successfully(
    stop_task, env, finished_prepare_task
):
    env["db"].get_task_by_task_id.return_value = finished_prepare_task

    run_task(stop_task, env)
    env["collectors"].delete.assert_called_once()
    env["collectors"].delete.assert_called_with(USER_UID, USER_SUID, POPID)


def test_should_finish_task_if_polling_prepare_task_finished_successfully(
    stop_task, env, finished_prepare_task
):
    env["db"].get_task_by_task_id.return_value = finished_prepare_task

    run_task(stop_task, env)
    env["db"].finish_task.assert_called_once()


def test_should_finish_task_if_polling_prepare_task_finished_with_error(
    stop_task, env, failed_prepare_task
):
    env["db"].get_task_by_task_id.return_value = failed_prepare_task

    run_task(stop_task, env)
    env["db"].finish_task.assert_called_once()

def test_should_not_try_to_delete_collector_if_polling_prepare_task_did_not_finish_successfully(
    stop_task, env, failed_prepare_task
):
    env["db"].get_task_by_task_id.return_value = failed_prepare_task
    run_task(stop_task, env)
    env["collectors"].delete.assert_not_called()


def test_should_not_try_to_finish_task_if_polling_prepare_task_pending(
    stop_task, env, pending_prepare_task
):
    env["db"].get_task_by_task_id.return_value = pending_prepare_task

    run_task(stop_task, env)
    env["db"].finish_task.assert_not_called()


def test_should_fail_task_in_case_of_error(stop_task, env):
    env["db"].get_task_by_task_id.side_effect = _throw_test_exception
    run_task(stop_task, env)
    env["db"].fail_task.assert_called_once()


def test_should_not_fail_task_in_case_of_no_error(stop_task, env, prepare_task):
    env["db"].get_task_by_task_id.return_value = prepare_task
    run_task(stop_task, env)
    env["db"].fail_task.assert_not_called()
