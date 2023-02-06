import urllib.request, urllib.parse, urllib.error

from rest_framework import status

import pytest

from fan.accounts.get import user_has_role_in_account, get_account_by_name
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, mock_directory_user):
    pass


def test_missing_user_id(tvm_api_client, org_id):
    response = do_request(tvm_api_client, None, org_id)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_org_id(tvm_api_client, user_id):
    response = do_request(tvm_api_client, user_id, None)
    assert_validation_error(response, "org_id", "not_found")


def test_user_is_not_in_organization(tvm_api_client, user_id, org_id):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_forbidden_error(response, "not_admin")


def test_user_is_not_admin(tvm_api_client, mock_directory_user, user_id, org_id):
    mock_directory_user.users[user_id] = "user"
    response = do_request(tvm_api_client, user_id, org_id)
    assert_forbidden_error(response, "not_admin")


def test_put_account(tvm_api_client, user_id, user_admin_in_directory, org_id):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"] == "org{}".format(org_id)
    assert response_data["org_id"] == org_id


def test_user_has_role_after_put(tvm_api_client, user_id, user_admin_in_directory, org_id):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    account = get_account_by_name(response_data["slug"])
    assert user_has_role_in_account(user_id, account)


def test_account_has_default_from_logins(tvm_api_client, user_id, user_admin_in_directory, org_id):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    account = get_account_by_name(response_data["slug"])
    assert len(account.from_logins) > 0


def test_put_account_twice(tvm_api_client, user_id, user_admin_in_directory, org_id):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"] == "org{}".format(org_id)
    assert response_data["org_id"] == org_id


def do_request(client, user_id=None, org_id=None):
    query_params = {}
    if user_id is not None:
        query_params["user_id"] = user_id
    if org_id is not None:
        query_params["org_id"] = org_id
    url = "/api/v1/account?" + urllib.parse.urlencode(query_params)
    return client.put(url)
