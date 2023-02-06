import urllib.request, urllib.parse, urllib.error

from rest_framework import status

import pytest

from fan.models.user import UserRole
from fan.models.account import Account
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_not_authenticated_error,
    assert_status_code,
    assert_lists_are_matching,
    assert_validation_error,
)


pytestmark = pytest.mark.django_db


def test_account_list(tvm_api_client, user_id, account_list):
    do_test_account_list(tvm_api_client, user_id, account_list)


def test_account_list_session_auth(auth_api_client, user_id, account_list):
    do_test_account_list(auth_api_client, user_id, account_list)


def test_account_list_wo_auth_localhost(
    api_client, user_id, account_list, disable_auth_on_loopback
):
    do_test_account_list(api_client, user_id, account_list)


def test_account_list_wo_auth(api_client, user_id):
    response = do_request(api_client, user_id)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_account_list_with_damaged_tvm_credentials(tvm_api_client_with_damaged_ticket, user_id):
    response = do_request(tvm_api_client_with_damaged_ticket, user_id)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_account_list_with_non_registered_tvm_source(
    tvm_api_client, user_id, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(tvm_api_client, user_id)
    assert_forbidden_error(response, "forbidden_service")


def test_account_list_with_missing_user_id(tvm_api_client):
    response = do_request(tvm_api_client)
    assert_validation_error(response, "user_id", "not_found")


@pytest.fixture
def account_list(user_id):
    accounts = [
        Account.objects.create(name="acc1"),
        Account.objects.create(name="acc2"),
    ]
    for account in accounts:
        UserRole.objects.create(user_id=user_id, account=account, role=UserRole.ROLES.USER)
    return accounts


def do_test_account_list(client, user_id, account_list):
    response = do_request(client, user_id)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    expected_accounts = [
        {"slug": account.name, "org_id": account.org_id} for account in account_list
    ]
    assert_lists_are_matching(response_data, expected_accounts)


def do_request(client, user_id=None):
    query_params = {}
    if user_id is not None:
        query_params["user_id"] = user_id
    url = "/api/v1/account-list?" + urllib.parse.urlencode(query_params)
    return client.get(url)
