import pytest
import urllib.request, urllib.parse, urllib.error
from rest_framework import status

from fan.models.account import Account
from fan.testutils.matchers import (
    assert_lists_are_matching,
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_forbidden_error,
)

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_directory_user):
    pass


@pytest.fixture
def accounts(org_id):
    return [
        Account.objects.create(name="acc1", org_id=org_id),
        Account.objects.create(name="acc2", org_id=org_id),
    ]


class TestOrgAccountList:
    def test_missing_user_id(self, tvm_api_client, org_id):
        response = do_request(tvm_api_client, None, org_id)
        assert_validation_error(response, "user_id", "not_found")

    def test_missing_org_id(self, tvm_api_client, user_id):
        response = do_request(tvm_api_client, user_id, None)
        assert_validation_error(response, "org_id", "not_found")

    def test_user_is_not_in_organization(self, tvm_api_client, mock_tvm, user_id, org_id):
        response = do_request(tvm_api_client, user_id, org_id)
        assert_forbidden_error(response, "not_admin")

    def test_user_is_not_admin(self, tvm_api_client, mock_directory_user, user_id, org_id):
        mock_directory_user.users[user_id] = "user"
        response = do_request(tvm_api_client, user_id, org_id)
        assert_forbidden_error(response, "not_admin")

    def test_user_is_admin_org_without_accounts(
        self, tvm_api_client, mock_tvm, user_id, user_admin_in_directory, org_id
    ):
        response = do_request(tvm_api_client, user_id, org_id)
        assert_status_code(response, status.HTTP_200_OK)
        response_data = response.json()
        assert response_data == []

    def test_user_is_admin_org_with_accounts(
        self, tvm_api_client, mock_tvm, accounts, user_id, user_admin_in_directory, org_id
    ):
        self.do_test_user_is_admin_org_with_accounts(
            tvm_api_client, mock_tvm, accounts, user_id, org_id
        )

    def test_user_is_admin_org_with_accounts_session_auth(
        self, auth_api_client, mock_tvm, accounts, user_id, user_admin_in_directory, org_id
    ):
        self.do_test_user_is_admin_org_with_accounts(
            auth_api_client, mock_tvm, accounts, user_id, org_id
        )

    def test_user_is_admin_org_with_accounts_wo_auth_localhost(
        self,
        api_client,
        mock_tvm,
        accounts,
        user_id,
        user_admin_in_directory,
        org_id,
        disable_auth_on_loopback,
    ):
        self.do_test_user_is_admin_org_with_accounts(
            api_client, mock_tvm, accounts, user_id, org_id
        )

    def test_user_is_admin_org_with_accounts_wo_auth(
        self, api_client, mock_tvm, accounts, user_id, user_admin_in_directory, org_id
    ):
        response = do_request(api_client, user_id, org_id)
        assert_not_authenticated_error(response, "Authentication credentials were not provided.")

    def test_user_is_admin_org_with_accounts_with_damaged_tvm_credentials(
        self,
        tvm_api_client_with_damaged_ticket,
        mock_tvm,
        accounts,
        user_id,
        user_admin_in_directory,
        org_id,
    ):
        response = do_request(tvm_api_client_with_damaged_ticket, user_id, org_id)
        assert_not_authenticated_error(response, "Authentication credentials were not provided.")

    def test_user_is_admin_org_with_accounts_with_non_registered_tvm_source(
        self,
        tvm_api_client,
        mock_tvm,
        accounts,
        user_id,
        user_admin_in_directory,
        org_id,
        tvm_clear_api_v1_allowed_services_list,
    ):
        response = do_request(tvm_api_client, user_id, org_id)
        assert_forbidden_error(response, "forbidden_service")

    def do_test_user_is_admin_org_with_accounts(self, client, mock_tvm, accounts, user_id, org_id):
        response = do_request(client, user_id, org_id)
        assert_status_code(response, status.HTTP_200_OK)
        response_data = response.json()
        expected_accounts = [
            {
                "slug": account.name,
                "org_id": account.org_id,
            }
            for account in accounts
        ]
        assert_lists_are_matching(response_data, expected_accounts)


def do_request(client, user_id, org_id):
    query_params = {}
    if user_id is not None:
        query_params["user_id"] = user_id
    if org_id is not None:
        query_params["org_id"] = org_id
    url = "/api/v1/org-account-list?" + urllib.parse.urlencode(query_params)
    return client.get(url)
