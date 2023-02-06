import urllib.request, urllib.parse, urllib.error
from rest_framework import status
import pytest
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)
from fan.models import UserRole


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, mock_directory_user, mock_directory_users):
    pass


@pytest.fixture()
def org_user_ids(user_ids, mock_directory_users):
    for user_id in user_ids:
        mock_directory_users.users[user_id] = "user"
    return user_ids


def test_missing_user_id(auth_api_client, account, user_ids):
    response = do_request(auth_api_client, None, account, user_ids)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_account_slug(auth_api_client, user_id, user_ids):
    response = do_request(auth_api_client, user_id, None, user_ids)
    assert_validation_error(response, "account_slug", "not_found")


def test_missing_user_ids_to_grant(auth_api_client, user_id, user_admin_in_directory, account):
    response = do_request(auth_api_client, user_id, account, None)
    assert_validation_error(response, "uids", "invalid_type")


def test_user_is_not_in_organization(auth_api_client, user_id, account, user_ids):
    response = do_request(auth_api_client, user_id, account, user_ids)
    assert_forbidden_error(response, "not_admin")


def test_user_is_not_admin(auth_api_client, mock_directory_user, user_id, account, user_ids):
    mock_directory_user.users[user_id] = "user"
    response = do_request(auth_api_client, user_id, account, user_ids)
    assert_forbidden_error(response, "not_admin")


def test_dont_grant_access_to_users_not_from_organization(
    auth_api_client, account, user_id, user_admin_in_directory, user_ids
):
    response = do_request(auth_api_client, user_id, account, user_ids)
    assert_status_code(response, status.HTTP_409_CONFLICT)
    response_data = response.json()
    assert response_data["error"] == "not_belongs_to_org"
    assert response_data["detail"] == "user test_uid1 does not belong to organization test_org_id"


def test_grant_access_to_users(
    auth_api_client, account, user_id, user_admin_in_directory, org_user_ids
):
    assert_users_dont_have_role(account, org_user_ids)
    response = do_request(auth_api_client, user_id, account, org_user_ids)
    assert_status_code(response, status.HTTP_200_OK)
    assert_users_have_role(account, org_user_ids)


def test_org_admin_without_role_in_account(
    auth_api_client, mock_directory_user, account, foreign_user_id, org_user_ids
):
    mock_directory_user.users[foreign_user_id] = "admin"
    response = do_request(auth_api_client, foreign_user_id, account, org_user_ids)
    assert_status_code(response, status.HTTP_200_OK)
    assert_users_have_role(account, org_user_ids)


def do_request(client, user_id=None, account=None, user_ids_to_grant=None):
    args = {"user_id": user_id, "account_slug": account.name if account else None}
    url = "/api/v1/account-user-list?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    return client.post(url, user_ids_to_grant, format="json")


def assert_users_have_role(account, user_ids):
    user_roles = UserRole.objects.filter(
        account=account, user_id__in=user_ids, role=UserRole.ROLES.USER
    )
    user_ids_with_role = [user_role.user_id for user_role in user_roles]
    assert sorted(user_ids) == sorted(user_ids_with_role)


def assert_users_dont_have_role(account, user_ids):
    user_roles = UserRole.objects.filter(
        account=account, user_id__in=user_ids, role=UserRole.ROLES.USER
    )
    user_ids_with_role = [user_role.user_id for user_role in user_roles]
    assert user_ids_with_role == []
