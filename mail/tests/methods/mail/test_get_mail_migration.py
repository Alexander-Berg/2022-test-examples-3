import pytest
from unittest.mock import MagicMock
from tractor.mail.models import (
    MigrationStats,
    OrgMigrationStatus,
    UserMigrationInfo,
    UserMigrationStatus,
)
from tractor.models import ExternalSecretStatus
from tractor.tests.fixtures.common import ORG_ID
from tractor_api.methods.mail.impl.get_migration import (
    calculate_migration_status,
    get_migration_stats,
    make_response,
)


@pytest.fixture
def env():
    return {"db": MagicMock()}


MIGRATION_INFO_LIST = [
    UserMigrationInfo(login="user1", status=UserMigrationStatus.PREPARING, error_reason=""),
    UserMigrationInfo(login="user2", status=UserMigrationStatus.SYNC_NEWEST, error_reason=""),
    UserMigrationInfo(login="user3", status=UserMigrationStatus.ERROR, error_reason=""),
]

NOT_LOADED_SECRET_STATUS = ExternalSecretStatus(provider="", external_secret_loaded=False)
LOADED_SECRET_STATUS = ExternalSecretStatus(provider="google", external_secret_loaded=True)


def test_migration_stats(env):
    env["db"].get_user_migration_statuses.return_value = MIGRATION_INFO_LIST
    stats = get_migration_stats(ORG_ID, env)
    assert stats.preparing == 1
    assert stats.initial_sync == 0
    assert stats.sync_newest == 1
    assert stats.stopping == 0
    assert stats.success == 0
    assert stats.error == 1


def test_status_is_not_started_if_has_no_users():
    stats = MigrationStats()
    assert calculate_migration_status(stats) == OrgMigrationStatus.NOT_STARTED


def test_status_is_in_inprogress_if_has_preparing_users():
    stats = MigrationStats(preparing=1, error=1, stopping=1, success=1)
    assert calculate_migration_status(stats) == OrgMigrationStatus.IN_PROGRESS


def test_status_is_stopping_if_has_only_stopping_and_finished_users():
    stats = MigrationStats(stopping=1, error=1, success=1)
    assert calculate_migration_status(stats) == OrgMigrationStatus.STOPPING


def test_status_is_error_if_all_users_finished_and_at_least_one_has_error():
    stats = MigrationStats(error=1, success=1)
    assert calculate_migration_status(stats) == OrgMigrationStatus.ERROR


def test_status_is_success_if_all_users_successfully_finished():
    stats = MigrationStats(success=1)
    assert calculate_migration_status(stats) == OrgMigrationStatus.SUCCESS


def test_make_response():
    response = make_response(
        MigrationStats(preparing=1, sync_newest=2, success=3),
        OrgMigrationStatus.NOT_STARTED,
        NOT_LOADED_SECRET_STATUS,
    )
    assert response["external_secret_loaded"] == False
    assert response["status"] == OrgMigrationStatus.NOT_STARTED.value
    assert response["user_stats"]["preparing"] == 1
    assert response["user_stats"]["sync_newest"] == 2
    assert response["user_stats"]["success"] == 3


def test_dont_set_provider_for_not_started_migration():
    response = make_response(
        MigrationStats(), OrgMigrationStatus.NOT_STARTED, NOT_LOADED_SECRET_STATUS
    )
    assert "provider" not in response


def test_set_custom_provider_if_migration_running_without_secret():
    response = make_response(
        MigrationStats(), OrgMigrationStatus.IN_PROGRESS, NOT_LOADED_SECRET_STATUS
    )
    assert response["provider"] == "custom"


def test_set_provider_from_secret():
    response = make_response(MigrationStats(), OrgMigrationStatus.IN_PROGRESS, LOADED_SECRET_STATUS)
    assert response["provider"] == LOADED_SECRET_STATUS.provider
