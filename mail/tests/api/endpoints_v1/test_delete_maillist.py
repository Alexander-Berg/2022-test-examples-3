import pytest
from urllib.parse import urlencode
from rest_framework import status
from rest_framework.test import APIClient
from fan.models import Maillist
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_not_authenticated_error,
    assert_status_code,
)


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


def test_on_account_with_maillist(user_id, account, maillist):
    response = do_request(user_id, account, maillist)
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account.maillists.all()) == 0


def test_on_account_with_maillists(user_id, account_with_almost_max_maillists_count):
    response = do_request(
        user_id,
        account_with_almost_max_maillists_count,
        account_with_almost_max_maillists_count.maillists.all()[0],
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account_with_almost_max_maillists_count.maillists.all()) == 3


def test_fail_wo_auth(user_id, account, maillist, use_api_client_wo_auth):
    response = do_request(user_id, account, maillist)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_fail_with_damaged_tvm_ticket(
    user_id, account, maillist, use_tvm_api_client_with_damaged_ticket
):
    response = do_request(user_id, account, maillist)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_fail_with_non_registered_tvm_source(
    user_id, account, maillist, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account, maillist)
    assert_forbidden_error(response, "forbidden_service")


def test_fail_with_forbidden_user(account, maillist):
    response = do_request("forbidden_user_id", account, maillist)
    assert_forbidden_error(response, "forbidden_user")


def test_fail_with_unexisted_account(user_id, account, maillist):
    account.name = "unexisted-account"
    response = do_request(user_id, account, maillist)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_fail_with_unexisted_maillist(user_id, account):
    response = do_request(user_id, account, "unexisted-maillist")
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def do_request(user_id, account, maillist):
    query_params = {
        "user_id": user_id,
        "account_slug": account.name,
        "maillist_slug": maillist.slug if isinstance(maillist, Maillist) else maillist,
    }
    url = "/api/v1/maillist?" + urlencode(
        {k: v for k, v in list(query_params.items()) if v is not None}
    )
    global client
    return client.delete(path=url)
