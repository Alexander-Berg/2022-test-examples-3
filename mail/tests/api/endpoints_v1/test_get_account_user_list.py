import urllib.request, urllib.parse, urllib.error
from rest_framework import status
import pytest
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, mock_directory_user):
    pass


def test_missing_user_id(auth_api_client, account):
    response = do_request(auth_api_client, None, account)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_account_slug(auth_api_client, user_id):
    response = do_request(auth_api_client, user_id, None)
    assert_validation_error(response, "account_slug", "not_found")


def test_user_is_not_in_organization(auth_api_client, user_id, account):
    response = do_request(auth_api_client, user_id, account)
    assert_forbidden_error(response, "not_admin")


def test_user_is_not_admin(auth_api_client, mock_directory_user, user_id, account):
    mock_directory_user.users[user_id] = "user"
    response = do_request(auth_api_client, user_id, account)
    assert_forbidden_error(response, "not_admin")


def test_account_without_users(
    auth_api_client, account_without_users, user_id, user_admin_in_directory
):
    response = do_request(auth_api_client, user_id, account_without_users)
    response_data = response.json()
    assert response_data == []


def test_account_with_users(
    auth_api_client, account_with_users, user_id, user_admin_in_directory, user_ids
):
    response = do_request(auth_api_client, user_id, account_with_users)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert sorted(response_data) == sorted(user_ids)


def test_org_admin_without_role(
    auth_api_client, mock_directory_user, foreign_user_id, account_with_users, user_ids
):
    mock_directory_user.users[foreign_user_id] = "admin"
    response = do_request(auth_api_client, foreign_user_id, account_with_users)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert sorted(response_data) == sorted(user_ids)


def do_request(client, user_id=None, account=None):
    args = {"user_id": user_id, "account_slug": account.name if account else None}
    url = "/api/v1/account-user-list?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    return client.get(url)
