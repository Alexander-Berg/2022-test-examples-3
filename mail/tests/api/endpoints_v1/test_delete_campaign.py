import pytest
import urllib.request, urllib.parse, urllib.error
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
    assert_wrong_state_error,
)
from rest_framework import status


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


def test_error_on_missing_user_id(account, campaign):
    response = _do_request(None, account.name, campaign.slug)
    assert_validation_error(response, "user_id", "not_found")


def test_error_on_forbidden_user(account, campaign):
    response = _do_request("forbidden_user_id", account.name, campaign.slug)
    assert_forbidden_error(response, "forbidden_user")


def test_error_on_missing_account_slug(user_id, campaign):
    response = _do_request(user_id, None, campaign.slug)
    assert_validation_error(response, "account_slug", "not_found")


def test_error_on_non_existent_account_slug(user_id, campaign):
    response = _do_request(user_id, "non-existent-account", campaign.slug)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_error_on_missing_campaign_slug(user_id, account):
    response = _do_request(user_id, account.name, None)
    assert_validation_error(response, "campaign_slug", "not_found")


def test_error_on_non_existent_campaign_slug(user_id, account):
    response = _do_request(user_id, account.name, "non-existent-campaign")
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_deletes_empty_campaign(user_id, account, campaign):
    response = _do_request(user_id, account.name, campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    assert _account_campaigns_count(account) == 0


def test_deletes_ready_campaign(user_id, account, ready_campaign):
    response = _do_request(user_id, account.name, ready_campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    assert _account_campaigns_count(account) == 0


def test_error_on_deleting_sending_campaign(user_id, account, sending_campaign):
    response = _do_request(user_id, account.name, sending_campaign.slug)
    assert_wrong_state_error(response, "sending", "draft")


def test_error_on_deleting_sent_campaign(user_id, account, sent_campaign):
    response = _do_request(user_id, account.name, sent_campaign.slug)
    assert_wrong_state_error(response, "sent", "draft")


def test_error_on_deleting_failed_campaign(user_id, account, failed_campaign):
    response = _do_request(user_id, account.name, failed_campaign.slug)
    assert_wrong_state_error(response, "failed", "draft")


def _do_request(user_id, account_slug, campaign_slug):
    global api_client
    args = {"user_id": user_id, "account_slug": account_slug, "campaign_slug": campaign_slug}
    url = "/api/v1/campaign?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    return api_client.delete(url)


def _account_campaigns_count(account):
    return account.campaign_set.all().count()
