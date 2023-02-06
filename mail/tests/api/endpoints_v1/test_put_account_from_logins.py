import urllib.request, urllib.parse, urllib.error
import pytest
from django.conf import settings
from rest_framework import status
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)


pytestmark = pytest.mark.django_db


@pytest.fixture
def prohibit_setting_empty_from_logins():
    saved_value = settings.ACCOUNT_FROM_LOGINS_COUNT_MIN
    settings.ACCOUNT_FROM_LOGINS_COUNT_MIN = 1
    yield
    settings.ACCOUNT_FROM_LOGINS_COUNT_MIN = saved_value


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, mock_directory_user):
    pass


@pytest.fixture
def from_logins():
    return ["hello", "no-reply"]


@pytest.fixture
def from_logins_too_long():
    return [str(uid) for uid in range(0, settings.ACCOUNT_FROM_LOGINS_COUNT_MAX + 1)]


def test_missing_user_id(tvm_api_client, account, from_logins):
    response = do_request(tvm_api_client, None, account, from_logins)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_account_slug(tvm_api_client, user_id, from_logins):
    response = do_request(tvm_api_client, user_id, None, from_logins)
    assert_validation_error(response, "account_slug", "not_found")


def test_missing_from_logins(tvm_api_client, user_id, account, user_admin_in_directory):
    response = do_request(tvm_api_client, user_id, account, None)
    assert_validation_error(response, "from_logins", "invalid_type")


def test_from_logins_contains_nonstring(tvm_api_client, user_id, account, user_admin_in_directory):
    response = do_request(tvm_api_client, user_id, account, ["valid", 123])
    assert_validation_error(response, "from_logins", "invalid_type")


def test_empty_from_logins(
    tvm_api_client, user_id, user_admin_in_directory, account, prohibit_setting_empty_from_logins
):
    response = do_request(tvm_api_client, user_id, account, [])
    assert_validation_error(response, "from_logins", "empty")


def test_from_logins_too_long(
    tvm_api_client, user_id, user_admin_in_directory, account, from_logins_too_long
):
    response = do_request(tvm_api_client, user_id, account, from_logins_too_long)
    assert_validation_error(response, "from_logins", "too_long")


def test_invalid_from_login(tvm_api_client, user_id, user_admin_in_directory, account):
    response = do_request(tvm_api_client, user_id, account, [";invalid_from_login;"])
    assert_validation_error(response, "from_logins", "invalid_email_localpart")


def test_user_is_not_in_organization(tvm_api_client, user_id, account, from_logins):
    response = do_request(tvm_api_client, user_id, account, from_logins)
    assert_forbidden_error(response, "not_admin")


def test_user_is_not_admin(tvm_api_client, mock_directory_user, user_id, account, from_logins):
    mock_directory_user.users[user_id] = "user"
    response = do_request(tvm_api_client, user_id, account, from_logins)
    assert_forbidden_error(response, "not_admin")


def test_set_from_logins(tvm_api_client, user_id, user_admin_in_directory, account, from_logins):
    response = do_request(tvm_api_client, user_id, account, from_logins)
    assert_status_code(response, status.HTTP_200_OK)
    assert sorted(response.json()) == sorted(from_logins)
    assert sorted(refresh_from_db(account).from_logins) == sorted(from_logins)


def do_request(client, user_id=None, account=None, from_logins=None):
    args = {"user_id": user_id, "account_slug": account.name if account else None}
    url = "/api/v1/account-from-logins?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    return client.put(url, from_logins, format="json")


def refresh_from_db(obj):
    obj.refresh_from_db()
    return obj
