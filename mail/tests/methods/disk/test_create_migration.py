from tractor_api.app.error_handling import AppException
from tractor_api.methods.disk.impl.create_migration import (
    _create_migration,
    SkipReason,
    _parse_request_data,
    _make_response,
)
from tractor.disk.models import UserMigration, UserMigrationStatus, TaskType, UserStatistics
from tractor.tests.fixtures.common import *
from tractor.tests.fixtures.common import GOOGLE_PROVIDER
from unittest.mock import MagicMock, ANY
import json
import pytest


@pytest.fixture
def env():
    env = {
        "db": MagicMock(),
        "yandex_directory": MagicMock(),
    }
    env["db"].get_user_migration.return_value = None
    env["yandex_directory"].get_users.return_value = [{"uid": UID, "email": EMAIL}]
    env["yandex_directory"].get_domain.return_value = DOMAIN
    return env


def csv_data_from_line_list(lines):
    return bytes("\n".join(lines), "utf-8")


def test_store_null_uid_for_missing_in_yandex(env):
    env["yandex_directory"].get_users.return_value = []
    skipped_logins = _create_migration(ORG_ID, GOOGLE_PROVIDER, [LOGIN], env)
    assert skipped_logins == {}
    user = {
        "uid": None,
        "login": LOGIN,
        "email": "{}@{}".format(LOGIN, DOMAIN),
    }
    list_task_inp = json.dumps({"user": user, "provider": GOOGLE_PROVIDER})
    env["db"].create_task.assert_called_with(
        type=TaskType.LIST, org_id=ORG_ID, domain=DOMAIN, worker_input=list_task_inp, cur=ANY
    )
    env["db"].create_user_migration.assert_called_with(ORG_ID, DOMAIN, LOGIN, ANY, ANY)


def test_create_new_migration(env):
    skipped_logins = _create_migration(ORG_ID, GOOGLE_PROVIDER, [LOGIN], env)
    assert skipped_logins == {}
    user = {
        "uid": UID,
        "login": LOGIN,
        "email": "{}@{}".format(LOGIN, DOMAIN),
    }
    list_task_inp = json.dumps({"user": user, "provider": GOOGLE_PROVIDER})
    env["db"].create_task.assert_called_with(
        type=TaskType.LIST, org_id=ORG_ID, domain=DOMAIN, worker_input=list_task_inp, cur=ANY
    )
    env["db"].create_user_migration.assert_called_with(ORG_ID, DOMAIN, LOGIN, ANY, ANY)


def test_create_migration_with_domain_in_logins(env):
    skipped_logins = _create_migration(
        ORG_ID, GOOGLE_PROVIDER, ["{}@{}".format(LOGIN, DOMAIN)], env
    )
    assert skipped_logins == {}
    env["db"].create_user_migration.assert_called_with(ORG_ID, DOMAIN, LOGIN, ANY, ANY)


def test_update_finished_migration(env):
    env["db"].get_user_migration.return_value = UserMigration(
        org_id=ORG_ID,
        domain=DOMAIN,
        login=LOGIN,
        status=UserMigrationStatus.SUCCESS,
        list_task_id=None,
        sync_task_ids=[],
        stats=UserStatistics(quota=999),
    )
    skipped_logins = _create_migration(ORG_ID, GOOGLE_PROVIDER, [LOGIN], env)
    assert skipped_logins == {}
    user = {
        "uid": UID,
        "login": LOGIN,
        "email": "{}@{}".format(LOGIN, DOMAIN),
    }
    list_task_inp = json.dumps({"user": user, "provider": GOOGLE_PROVIDER})

    env["db"].create_task.assert_called_with(
        type=TaskType.LIST, org_id=ORG_ID, domain=DOMAIN, worker_input=list_task_inp, cur=ANY
    )
    env["db"].reset_user_migration.assert_called_with(ORG_ID, LOGIN, ANY, ANY)


def test_response_json_serializable(env):
    env["yandex_directory"].get_users.return_value = []
    skipped_logins = _create_migration(ORG_ID, GOOGLE_PROVIDER, [LOGIN], env)
    response = _make_response(skipped_logins)
    try:
        json.dumps(response)
    except TypeError:
        pytest.fail("skipped_logins are not JSON serializable")


TEST_CSV_DATA = [
    (csv_data_from_line_list(["login"]), []),
    (csv_data_from_line_list(["login", "test_login"]), ["test_login"]),
    (csv_data_from_line_list(["login;", "test_login;"]), ["test_login"]),
    (csv_data_from_line_list(["login,", "test_login,"]), ["test_login"]),
    (csv_data_from_line_list(["login", "test_login;"]), ["test_login"]),
    (csv_data_from_line_list(["login;,;", "test_login;,;"]), ["test_login"]),
]


@pytest.mark.parametrize("data, logins", TEST_CSV_DATA)
def test_parse_request_data(data, logins):
    assert _parse_request_data(data) == logins


TEST_BAD_CSV_DATA = [
    (csv_data_from_line_list(["column_name"]), "csv_bad_header_row"),
    (csv_data_from_line_list(["column_name;"]), "csv_bad_header_row"),
    (csv_data_from_line_list(["login", "test_login, test_login2"]), "csv_bad_file"),
    (csv_data_from_line_list(["login,password", "test_login,password"]), "csv_bad_header_row"),
]


@pytest.mark.parametrize("data, error_code", TEST_BAD_CSV_DATA)
def test_parse_request_data_bad_csv(data, error_code):
    with pytest.raises(AppException) as e:
        _parse_request_data(data)
    assert e.value.status_code == 400
    assert e.value.error_code == error_code


TEST_RESPONSE_DATA = [
    ({}, {"skipped_logins": []}),
    (
        {LOGIN: SkipReason.NOT_FOUND_IN_YANDEX},
        {"skipped_logins": [{"login": LOGIN, "reason": SkipReason.NOT_FOUND_IN_YANDEX}]},
    ),
]


@pytest.mark.parametrize("skipped_logins,response", TEST_RESPONSE_DATA)
def test_make_response(skipped_logins, response):
    assert response == _make_response(skipped_logins)
