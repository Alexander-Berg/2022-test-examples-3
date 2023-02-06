from tractor_api.methods.disk.migration import _export_migration
from tractor_api.methods.disk.impl.get_migration import (
    _fill_response_with_migration_statuses,
    _fill_response_with_external_secret_status,
    _migration_status,
)
from tractor.util.dataclasses import name_of_field
from tractor.error import NOT_ENOUGH_QUOTA
from tractor.models import ExternalSecretStatus
from tractor.disk.models import (
    UserMigrationStatus,
    UserMigrationInfo,
    UserMigrationExportInfo,
    OrgMigrationStatus,
    MigrationStats,
    UserStatistics,
)
from unittest.mock import MagicMock
import dataclasses
import pytest
import uuid


@pytest.fixture
def env():
    env = {
        "db": MagicMock(),
    }
    env["db"].get_user_migration_statuses.return_value = None
    return env


@pytest.fixture
def org_id():
    return "12345"


def _login():
    return str(uuid.uuid4())[:8]


def _not_enough_quota():
    return _error(NOT_ENOUGH_QUOTA)


def _error(reason=""):
    return UserMigrationInfo(login=_login(), status=UserMigrationStatus.ERROR, error_reason=reason)


def _listing():
    return UserMigrationInfo(login=_login(), status=UserMigrationStatus.LISTING)


def _syncing():
    return UserMigrationInfo(login=_login(), status=UserMigrationStatus.SYNCING)


def _canceling():
    return UserMigrationInfo(login=_login(), status=UserMigrationStatus.CANCELING)


def _success():
    return UserMigrationInfo(login=_login(), status=UserMigrationStatus.SUCCESS)


def _migration_stats(**kwargs):
    stats = dataclasses.asdict(MigrationStats())
    stats.update(kwargs)
    return stats


TEST_DATA_MIGRATION = [
    ([_success(), _success()], OrgMigrationStatus.SUCCESS, _migration_stats(success=2)),
    ([_error(), _success()], OrgMigrationStatus.ERROR, _migration_stats(success=1, error=1)),
    (
        [_listing(), _error(), _success()],
        OrgMigrationStatus.IN_PROGRESS,
        _migration_stats(success=1, error=1, preparing=1),
    ),
    (
        [_syncing(), _error(), _success()],
        OrgMigrationStatus.IN_PROGRESS,
        _migration_stats(success=1, in_progress=1, error=1),
    ),
    (
        [_syncing(), _canceling()],
        OrgMigrationStatus.IN_PROGRESS,
        _migration_stats(in_progress=1, canceling=1),
    ),
    (
        [_canceling(), _error(), _success()],
        OrgMigrationStatus.CANCELING,
        _migration_stats(canceling=1, success=1, error=1),
    ),
    (
        [_not_enough_quota()],
        OrgMigrationStatus.ERROR,
        _migration_stats(not_enough_quota=1),
    ),
    (
        [],
        OrgMigrationStatus.NOT_STARTED,
        _migration_stats(),
    ),
]


@pytest.mark.parametrize("db_result,status,stats", TEST_DATA_MIGRATION)
def test_get_migration(db_result, status, stats, env, org_id):
    env["db"].get_user_migration_statuses.return_value = db_result
    response = {}
    _fill_response_with_migration_statuses(org_id, env, response)
    assert response.get("status") == status.value
    assert response.get("user_stats") == stats


TEST_MIGRATION_LIST = [
    UserMigrationExportInfo(
        login="leslie",
        status=UserMigrationStatus.LISTING,
        stats=UserStatistics(files_size=338, files_count=188, quota=1000),
    ),
    UserMigrationExportInfo(
        login="sydney",
        status=UserMigrationStatus.SYNCING,
        stats=UserStatistics(files_size=338, files_count=198, quota=1000),
    ),
    UserMigrationExportInfo(
        login="casey", status=UserMigrationStatus.CANCELING, stats=UserStatistics()
    ),
    UserMigrationExportInfo(
        login="eric",
        status=UserMigrationStatus.ERROR,
        error_reason=NOT_ENOUGH_QUOTA,
        stats=UserStatistics(files_size=338, files_count=188, quota=340),
    ),
    UserMigrationExportInfo(
        login="susan",
        status=UserMigrationStatus.SUCCESS,
        stats=UserStatistics(files_size=338, files_count=198, quota=1000),
    ),
]
CSV_RESULT = (
    """
login,status,error_reason,yandex_disk_quota,external_disk_files_size,external_disk_files_count
leslie,listing,"",1000,338,188
sydney,syncing,"",1000,338,198
casey,canceling,"",,,
eric,error,"""
    + '"'
    + NOT_ENOUGH_QUOTA
    + '"'
    + """,340,338,188
susan,success,"",1000,338,198
"""
)[1:]


def test_export_migration(env, org_id):
    env["db"].get_user_migrations_for_export = MagicMock(return_value=TEST_MIGRATION_LIST)
    response: str = _export_migration(org_id, env)
    assert response == CSV_RESULT


def _external_secret_status(provider, external_secret_loaded):
    resp = {}
    resp["provider"] = provider
    resp["external_secret_loaded"] = external_secret_loaded
    return resp


TEST_DATA_EXTERNAL_SECRET_STATUS = [
    (
        ExternalSecretStatus("google", True),
        _external_secret_status(provider="google", external_secret_loaded=True),
    ),
    (
        ExternalSecretStatus(None, False),
        _external_secret_status(provider=None, external_secret_loaded=False),
    ),
]


@pytest.mark.parametrize("db_result,resp", TEST_DATA_EXTERNAL_SECRET_STATUS)
def test_get_external_secret_status(db_result, resp, env, org_id):
    env["db"].get_external_secret_status_any_provider.return_value = db_result
    response = {}
    _fill_response_with_external_secret_status(org_id, env, response)
    assert response.get("provider") == resp["provider"]
    assert response.get("external_secret_loaded") == resp["external_secret_loaded"]


@pytest.mark.parametrize("field", dataclasses.fields(MigrationStats), ids=name_of_field)
def test_any_stats_counter_being_non_zero_makes_migration_considered_started(
    field: dataclasses.Field,
):
    stats = MigrationStats(**{field.name: 1})
    assert _migration_status(stats) != OrgMigrationStatus.NOT_STARTED
