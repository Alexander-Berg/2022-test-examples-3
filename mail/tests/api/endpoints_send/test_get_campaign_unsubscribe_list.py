import pytest
import urllib.request, urllib.parse, urllib.error
from rest_framework import status
from fan.testutils.matchers import assert_status_code, assert_validation_error


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


def test_unexisted_account(campaign):
    response = do_request("unexisted_account_slug", campaign.slug)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_unexisted_campaign(account):
    response = do_request(account.name, "unexisted_campaign_slug")
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_empty_account_slug(campaign):
    response = do_request("", campaign.slug)
    assert_validation_error(response, "account_slug", "empty")


def test_empty_campaign_slug(account):
    response = do_request(account.name, "")
    assert_validation_error(response, "campaign_slug", "empty")


def test_with_unsubscribers(account, campaign_with_unsubscribers):
    response = do_request(account.name, campaign_with_unsubscribers.slug)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 3


def test_without_unsubscribers(account, campaign):
    response = do_request(account.name, campaign.slug)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 0


def do_request(account_slug, campaign_slug):
    args = {
        "account_slug": account_slug,
        "campaign_slug": campaign_slug,
    }
    url = "/api/send/campaign-unsubscribe-list?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.get(url)
