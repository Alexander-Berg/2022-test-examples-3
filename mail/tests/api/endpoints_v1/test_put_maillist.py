import pytest
from django.conf import settings
from urllib.parse import urlencode
from rest_framework import status
from rest_framework.test import APIClient
from fan.models import Maillist
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
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


@pytest.fixture
def title():
    return "Новое название списка рассылки"


def test_response_json(user_id, account, maillist, title):
    response = do_request(user_id, account, maillist, title)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"]
    assert response_data["title"] == title
    assert response_data["filename"] == "maillist.csv"
    assert response_data["size"] == 2
    assert response_data["preview"] == [
        {"name": "Иван", "email": "a@b.c", "value": "100"},
        {"name": "Марья", "email": "d@e.f", "value": "50"},
    ]


def test_fail_wo_auth(user_id, account, maillist, title, use_api_client_wo_auth):
    response = do_request(user_id, account, maillist, title)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_fail_with_damaged_tvm_ticket(
    user_id, account, maillist, title, use_tvm_api_client_with_damaged_ticket
):
    response = do_request(user_id, account, maillist, title)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_fail_with_non_registered_tvm_source(
    user_id, account, maillist, title, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account, maillist, title)
    assert_forbidden_error(response, "forbidden_service")


def test_fail_with_forbidden_user(account, maillist, title):
    response = do_request("forbidden_user_id", account, maillist, title)
    assert_forbidden_error(response, "forbidden_user")


def test_fail_with_unexisted_account(user_id, account, maillist, title):
    account.name = "unexisted-account"
    response = do_request(user_id, account, maillist, title)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_fail_with_unexisted_maillist(user_id, account, title):
    response = do_request(user_id, account, "unexisted-maillist", title)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_fail_on_empty_title(user_id, account, maillist):
    response = do_request(user_id, account, maillist, title="")
    assert_validation_error(response, "title", "empty")


def test_fail_on_duplicated_title(user_id, account, maillist):
    response = do_request(user_id, account, maillist, title=maillist.title)
    assert_validation_error(response, "title", "duplicated")


def test_fail_on_too_long_title(user_id, account, maillist):
    response = do_request(
        user_id, account, maillist, title="T" * (settings.MAILLIST_TITLE_MAX_LENGTH + 1)
    )
    assert_validation_error(response, "title", "too_long")


def do_request(user_id, account, maillist, title):
    query_params = {
        "user_id": user_id,
        "account_slug": account.name,
        "maillist_slug": maillist.slug if isinstance(maillist, Maillist) else maillist,
        "title": title,
    }
    url = "/api/v1/maillist?" + urlencode(
        {k: v for k, v in list(query_params.items()) if v is not None}
    )
    global client
    return client.put(path=url)
