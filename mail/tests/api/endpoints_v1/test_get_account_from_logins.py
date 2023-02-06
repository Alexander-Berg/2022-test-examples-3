import urllib.request, urllib.parse, urllib.error
import pytest
from rest_framework import status
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)


pytestmark = pytest.mark.django_db


def test_missing_user_id(tvm_api_client, account):
    response = do_request(tvm_api_client, None, account)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_account_slug(tvm_api_client, user_id):
    response = do_request(tvm_api_client, user_id, None)
    assert_validation_error(response, "account_slug", "not_found")


def test_get_from_logins(tvm_api_client, user_id, account):
    response = do_request(tvm_api_client, user_id, account)
    assert_status_code(response, status.HTTP_200_OK)
    assert sorted(response.json()) == sorted(account.from_logins)


def test_user_does_not_have_role(tvm_api_client, foreign_user_id, account):
    response = do_request(tvm_api_client, foreign_user_id, account)
    assert_forbidden_error(response, "forbidden_user")


def do_request(client, user_id=None, account=None):
    args = {"user_id": user_id, "account_slug": account.name if account else None}
    url = "/api/v1/account-from-logins?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    return client.get(url)
