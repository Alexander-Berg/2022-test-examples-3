import pytest
from urllib.parse import urlencode
from rest_framework import status
from rest_framework.test import APIClient
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_not_authenticated_error,
    assert_status_code,
)
from fan.testutils.utils import format_nullable_datetime


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(tvm_api_client):
    global client
    client = tvm_api_client
    yield
    client = None


@pytest.fixture
def use_api_client_wo_auth():
    global client
    client = APIClient()
    yield
    client = None


@pytest.fixture
def use_tvm_api_client_with_damaged_ticket(tvm_api_client_with_damaged_ticket):
    global client
    client = tvm_api_client_with_damaged_ticket
    yield
    client = None


def test_on_account_wo_maillist(user_id, account):
    response = do_request(user_id, account)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data == []


def test_response_json(user_id, account, maillist):
    response = do_request(user_id, account)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert len(response_data) == 1
    assert response_data[0]["slug"]
    assert "Список рассылки" in response_data[0]["title"]
    assert response_data[0]["filename"] == "maillist.csv"
    assert response_data[0]["size"] == 2
    assert response_data[0]["created_at"] == format_nullable_datetime(maillist.created_at)
    assert response_data[0]["modified_at"] == format_nullable_datetime(maillist.modified_at)


def test_on_account_with_maillists(user_id, account_with_almost_max_maillists_count):
    response = do_request(user_id, account_with_almost_max_maillists_count)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert len(response_data) == 4


def test_fail_wo_auth(user_id, account, use_api_client_wo_auth):
    response = do_request(user_id, account)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_fail_with_damaged_tvm_ticket(user_id, account, use_tvm_api_client_with_damaged_ticket):
    response = do_request(user_id, account)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_fail_with_non_registered_tvm_source(
    user_id, account, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account)
    assert_forbidden_error(response, "forbidden_service")


def test_fail_with_forbidden_user(account):
    response = do_request("forbidden_user_id", account)
    assert_forbidden_error(response, "forbidden_user")


def test_fail_with_unexisted_account(user_id, account):
    account.name = "invalid"
    response = do_request(user_id, account)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def do_request(user_id, account):
    query_params = {
        "user_id": user_id,
        "account_slug": account.name,
    }
    url = "/api/v1/maillist-list?" + urlencode(
        {k: v for k, v in list(query_params.items()) if v is not None}
    )
    global client
    return client.get(path=url)
