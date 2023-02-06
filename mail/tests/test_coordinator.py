import traceback
from pytest import fixture
from unittest.mock import ANY, MagicMock
from tractor.mail.models import TaskType, UserMigration, UserMigrationStatus
from tractor.models import Task, TaskWorkerStatus
from tractor.tests.fixtures.common import *
from tractor_mail.impl.coordinator import (
    process_migration_in_preparing_status,
    process_migration_in_stopping_status,
)
from tractor.error import (
    MIGRATION_WAS_STOPPED_BEFORE_IT_WAS_FINISHED,
    PREPARING_ERROR,
    STOPPING_ERROR,
)


@fixture
def finished_prepare_task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=ANY,
        type=TaskType.PREPARE,
        input=ANY,
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.SUCCESS,
        worker_ts=ANY,
        worker_output={},
    )


@fixture
def failed_prepare_task(finished_prepare_task):
    finished_prepare_task.worker_status = TaskWorkerStatus.ERROR
    return finished_prepare_task


@fixture
def finished_stop_task():
    return Task(
        task_id=TASK_ID,
        org_id=ORG_ID,
        domain=DOMAIN,
        created_ts=ANY,
        type=TaskType.PREPARE,
        input=ANY,
        canceled=False,
        worker_id=None,
        worker_status=TaskWorkerStatus.SUCCESS,
        worker_ts=ANY,
        worker_output="",
    )


@fixture
def failed_stop_task(finished_stop_task):
    finished_stop_task.worker_status = TaskWorkerStatus.ERROR
    return finished_stop_task


@fixture
def preparing_migration():
    return UserMigration(
        ORG_ID,
        DOMAIN,
        LOGIN,
        UserMigrationStatus.PREPARING,
        TASK_ID,
        TASK_ID,
        {},
        "",
    )


@fixture
def stopping_migration(preparing_migration):
    preparing_migration.status = UserMigrationStatus.STOPPING
    return preparing_migration


@fixture
def env():
    env = {
        "db": MagicMock(),
        "logger": MagicMock(),
    }
    yield env

    if env["logger"].exception.called:
        e = env["logger"].exception.call_args.kwargs["exception"]
        traceback.print_tb(e.__traceback__)


def test_process_migration_in_preparing_status_should_move_migration_to_initial_sync_if_task_finished_successfully(
    preparing_migration, finished_prepare_task, env
):
    env["db"].get_task_by_task_id.return_value = finished_prepare_task
    process_migration_in_preparing_status(env, preparing_migration, ANY)
    env["db"].move_migration_to_new_status.assert_called_with(
        preparing_migration.org_id,
        preparing_migration.login,
        preparing_migration.status,
        UserMigrationStatus.INITIAL_SYNC,
        ANY,
    )


def test_process_migration_in_preparing_status_should_move_migration_to_error_if_task_finished_with_error(
    preparing_migration, failed_prepare_task, env
):
    env["db"].get_task_by_task_id.return_value = failed_prepare_task
    process_migration_in_preparing_status(env, preparing_migration, ANY)
    env["db"].move_migration_to_new_status.assert_called_with(
        preparing_migration.org_id,
        preparing_migration.login,
        preparing_migration.status,
        UserMigrationStatus.ERROR,
        ANY,
        error_reason=ANY,
    )


def test_process_migration_in_preparing_status_should_set_error_from_worker_output_to_migration_if_task_finished_with_error(
    preparing_migration, failed_prepare_task, env
):
    env["db"].get_task_by_task_id.return_value = failed_prepare_task
    process_migration_in_preparing_status(env, preparing_migration, ANY)
    env["db"].move_migration_to_new_status.assert_called_with(
        ANY,
        ANY,
        ANY,
        ANY,
        ANY,
        error_reason=PREPARING_ERROR,
    )


def test_process_migration_in_stopping_status_should_move_migration_to_error_if_task_finished_with_error(
    stopping_migration, failed_stop_task, env
):
    env["db"].get_task_by_task_id.return_value = failed_stop_task
    process_migration_in_stopping_status(env, stopping_migration, ANY)
    env["db"].move_migration_to_new_status.assert_called_with(
        stopping_migration.org_id,
        stopping_migration.login,
        stopping_migration.status,
        UserMigrationStatus.ERROR,
        ANY,
        error_reason=ANY,
    )


def test_process_migration_in_stopping_status_should_set_error_from_worker_output_to_migration_if_task_finished_with_error(
    stopping_migration, failed_stop_task, env
):
    env["db"].get_task_by_task_id.return_value = failed_stop_task
    process_migration_in_stopping_status(env, stopping_migration, ANY)
    env["db"].move_migration_to_new_status.assert_called_with(
        ANY,
        ANY,
        ANY,
        ANY,
        ANY,
        error_reason=STOPPING_ERROR,
    )


def test_process_migration_in_preparing_status_should_move_migration_to_stopped_if_prev_status_is_sync_newest(
    stopping_migration, finished_stop_task, env
):
    finished_stop_task.input = {"previous_migration_status": UserMigrationStatus.SYNC_NEWEST.value}
    env["db"].get_task_by_task_id.return_value = finished_stop_task
    process_migration_in_stopping_status(env, stopping_migration, ANY)
    env["db"].move_migration_to_new_status.assert_called_with(
        stopping_migration.org_id,
        stopping_migration.login,
        stopping_migration.status,
        UserMigrationStatus.STOPPED,
        ANY,
    )


def test_process_migration_in_preparing_status_should_move_migration_to_error_if_prev_status_is_distinct_from_sync_newest(
    stopping_migration, finished_stop_task, env
):
    finished_stop_task.input = {"previous_migration_status": UserMigrationStatus.INITIAL_SYNC.value}
    env["db"].get_task_by_task_id.return_value = finished_stop_task
    process_migration_in_stopping_status(env, stopping_migration, ANY)
    env["db"].move_migration_to_new_status.assert_called_with(
        ANY,
        ANY,
        ANY,
        ANY,
        ANY,
        error_reason=MIGRATION_WAS_STOPPED_BEFORE_IT_WAS_FINISHED,
    )
