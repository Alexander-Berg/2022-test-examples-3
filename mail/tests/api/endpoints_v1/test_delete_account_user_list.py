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
def default_fixtures(mock_tvm, mock_directory_user):
    pass


def test_missing_user_id(auth_api_client, account, user_ids):
    response = do_request(auth_api_client, None, account, user_ids)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_account_slug(auth_api_client, user_id, user_admin_in_directory, user_ids):
    response = do_request(auth_api_client, user_id, None, user_ids)
    assert_validation_error(response, "account_slug", "not_found")


def test_missing_user_ids_to_revoke(auth_api_client, user_id, user_admin_in_directory, account):
    response = do_request(auth_api_client, user_id, account, None)
    assert_validation_error(response, "uids", "invalid_type")


def test_user_is_not_in_organization(auth_api_client, user_id, account, user_ids):
    response = do_request(auth_api_client, user_id, account, user_ids)
    assert_forbidden_error(response, "not_admin")


def test_user_is_not_admin(auth_api_client, mock_directory_user, user_id, account, user_ids):
    mock_directory_user.users[user_id] = "user"
    response = do_request(auth_api_client, user_id, account, user_ids)
    assert_forbidden_error(response, "not_admin")


def test_revoke_access(
    auth_api_client, account_with_users, user_id, user_admin_in_directory, user_ids
):
    assert_users_have_role(account_with_users, user_ids)
    response = do_request(auth_api_client, user_id, account_with_users, user_ids)
    assert_status_code(response, status.HTTP_200_OK)
    assert_users_dont_have_role(account_with_users, user_ids)


def test_org_admin_without_role(
    auth_api_client, mock_directory_user, account_with_users, foreign_user_id, user_ids
):
    mock_directory_user.users[foreign_user_id] = "admin"
    response = do_request(auth_api_client, foreign_user_id, account_with_users, user_ids)
    assert_status_code(response, status.HTTP_200_OK)
    assert_users_dont_have_role(account_with_users, user_ids)


def do_request(client, user_id=None, account=None, user_ids_to_revoke=None):
    args = {"user_id": user_id, "account_slug": account.name if account else None}
    url = "/api/v1/account-user-list?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    return client.delete(url, user_ids_to_revoke, format="json")


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
