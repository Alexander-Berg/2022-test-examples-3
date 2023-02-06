from datetime import datetime
from unittest.mock import ANY, MagicMock
import pytest
from tractor.tests.fixtures.common import *
from tractor.models import Task, TaskWorkerStatus
from tractor.error import MAIL_SYNC_ERROR
from tractor.yandex_services.collectors import CollectorInfo
from tractor.mail.models import (
    MessagesCollectionStats,
    UserMigration,
    UserMigrationStatus,
    TaskType,
)
from tractor_mail.impl.collect_status_updater import process_migration

PREPARE_TASK_ID = "test_prepare_task_id"
SUID = "test_suid"
POPID = "test_popid"

USER_MIGRATION = UserMigration(
    ORG_ID, DOMAIN, LOGIN, UserMigrationStatus.INITIAL_SYNC, PREPARE_TASK_ID, None, None, None
)

PREPARE_TASK = Task(
    task_id=PREPARE_TASK_ID,
    org_id=ORG_ID,
    domain=DOMAIN,
    created_ts=datetime.now(),
    type=TaskType.PREPARE,
    input={},
    canceled=False,
    worker_id=None,
    worker_status=TaskWorkerStatus.SUCCESS,
    worker_ts=None,
    worker_output={"uid": UID, "suid": SUID, "popid": POPID},
)

COLLECTOR_INFO = CollectorInfo(
    bad_retries=0,
    error_status="ok",
    is_oauth=False,
    is_on=1,
    last_connect="",
    last_msg_count=0,
    login=LOGIN,
    popid=POPID,
    server_info=None,
)

COLLECTOR_WITH_PERM_ERROR_INFO = CollectorInfo(
    bad_retries=0,
    error_status="ok",
    is_oauth=False,
    is_on=3,
    last_connect="",
    last_msg_count=0,
    login=LOGIN,
    popid=POPID,
    server_info=None,
)

INBOX_FOLDER = "INBOX"
INBOX_STATS = MessagesCollectionStats(source_count=10, collected_count=5, failed_count=1)


@pytest.fixture
def env():
    env = {
        "db": MagicMock(),
        "collectors": MagicMock(),
    }
    env["db"].get_task_by_task_id.return_value = PREPARE_TASK
    env["collectors"].list.return_value = [COLLECTOR_INFO]
    env["collectors"].status.return_value = {INBOX_FOLDER: INBOX_STATS}
    return env


def test_update_stats(env):
    process_migration(USER_MIGRATION, env)

    expected_stats = {
        "folders": {
            INBOX_FOLDER: {
                "src_mailbox_messages_count": INBOX_STATS.source_count,
                "collected_messages_count": INBOX_STATS.collected_count,
                "failed_to_collect_messages_count": INBOX_STATS.failed_count,
            }
        }
    }
    env["db"].update_stats.assert_called_with(ORG_ID, LOGIN, expected_stats, ANY)


def test_move_to_error_state(env):
    env["collectors"].list.return_value = [COLLECTOR_WITH_PERM_ERROR_INFO]
    process_migration(USER_MIGRATION, env)
    env["db"].mark_migration_finished.assert_called_with(
        ORG_ID, LOGIN, UserMigrationStatus.ERROR, MAIL_SYNC_ERROR, ANY
    )


def test_dont_call_status_if_collector_in_perm_error(env):
    env["collectors"].list.return_value = [COLLECTOR_WITH_PERM_ERROR_INFO]
    process_migration(USER_MIGRATION, env)
    env["collectors"].status.assert_not_called()


def test_move_to_sync_newest_state_if_no_new_messages_in_last_iteration(env):
    process_migration(USER_MIGRATION, env)
    env["db"].move_migration_to_new_status.assert_called_with(ORG_ID, LOGIN, UserMigrationStatus.INITIAL_SYNC, UserMigrationStatus.SYNC_NEWEST, ANY)
