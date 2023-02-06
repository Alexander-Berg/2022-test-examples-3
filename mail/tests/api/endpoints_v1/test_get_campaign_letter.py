import pytest
from rest_framework import status
import urllib.request, urllib.parse, urllib.error

from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_resource_does_not_exist,
    assert_forbidden_error,
)

pytestmark = pytest.mark.django_db


def test_valid_html(tvm_api_client, user_id, account, campaign_with_letter):
    do_test_valid_html(tvm_api_client, user_id, account, campaign_with_letter)


def test_valid_html_session_auth(auth_api_client, user_id, account, campaign_with_letter):
    do_test_valid_html(auth_api_client, user_id, account, campaign_with_letter)


def test_valid_html_wo_auth_localhost(
    api_client, user_id, account, campaign_with_letter, disable_auth_on_loopback
):
    do_test_valid_html(api_client, user_id, account, campaign_with_letter)


def test_valid_html_wo_auth(api_client, user_id, account, campaign_with_letter):
    response = do_request(api_client, user_id, account.name, campaign_with_letter.slug)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_valid_html_with_damaged_tvm_credentials(
    tvm_api_client_with_damaged_ticket, user_id, account, campaign_with_letter
):
    response = do_request(
        tvm_api_client_with_damaged_ticket, user_id, account.name, campaign_with_letter.slug
    )
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_valid_html_with_non_registered_tvm_source(
    tvm_api_client, user_id, account, campaign_with_letter, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(tvm_api_client, user_id, account.name, campaign_with_letter.slug)
    assert_forbidden_error(response, "forbidden_service")


def test_missing_user_id_param(tvm_api_client, account, campaign):
    response = do_request(tvm_api_client, None, account.name, campaign.slug)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_account_slug_param(tvm_api_client, user_id, campaign):
    response = do_request(tvm_api_client, user_id, None, campaign.slug)
    assert_validation_error(response, "account_slug", "not_found")


def test_missing_campaign_slug_param(tvm_api_client, user_id, account):
    response = do_request(tvm_api_client, user_id, account.name, None)
    assert_validation_error(response, "campaign_slug", "not_found")


def test_empty_letter(tvm_api_client, user_id, account, campaign):
    response = do_request(tvm_api_client, user_id, account.name, campaign.slug)
    assert_resource_does_not_exist(response, "letter", "empty")


def test_forbidden_user(tvm_api_client, account, campaign_with_letter):
    response = do_request(
        tvm_api_client, "forbidden_user_id", account.name, campaign_with_letter.slug
    )
    assert_forbidden_error(response, "forbidden_user")


def do_test_valid_html(client, user_id, account, campaign_with_letter):
    response = do_request(client, user_id, account.name, campaign_with_letter.slug)
    assert_status_code(response, status.HTTP_200_OK)
    assert "<html>" in response.content.decode("utf-8"), "doesn't contain HTML tag"
    assert "{{ unsubscribe_link }}" in response.content.decode(
        "utf-8"
    ), "doesn't contain unsubscribe_link placeholder"


def do_request(client, user_id, account_slug, campaign_slug):
    args = {"user_id": user_id, "account_slug": account_slug, "campaign_slug": campaign_slug}
    url = "/api/v1/campaign-letter?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v}
    )
    return client.get(url)
