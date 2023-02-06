import json
import pytest
import urllib.request, urllib.parse, urllib.error
from rest_framework import status
from fan.links.unsubscribe import decode_unsubscribe_code2
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


def test_with_recipients(account, campaign):
    recipients = ["a@test.ru", "b@test.ru"]
    response = do_request(account.name, campaign.slug, recipients)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 2


def test_letter_secret(account, campaign):
    recipients = ["john_doe@test.ru"]
    response = do_request(account.name, campaign.slug, recipients)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    recipient, campaign_id, letter_id, _, for_testing = decode_unsubscribe_code2(
        response_data[0]["secret"]
    )
    assert recipient == "john_doe@test.ru"
    assert campaign_id == campaign.id
    assert letter_id == campaign.default_letter.id
    assert for_testing == False


def test_with_zero_recipients(account, campaign):
    recipients = []
    response = do_request(account.name, campaign.slug, recipients)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_data == []


def test_with_missing_recipients(account, campaign):
    response = do_request(account.name, campaign.slug, None)
    response_data = response.json()
    assert_validation_error(response, "recipients", "not_found")


def do_request(account_slug, campaign_slug, recipients=None):
    args = {
        "account_slug": account_slug,
        "campaign_slug": campaign_slug,
    }
    url = "/api/send/campaign-template-params?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    if recipients is not None:
        return api_client.generic(
            method="GET",
            path=url,
            data=json.dumps({"recipients": recipients}),
            content_type="application/json",
        )
    else:
        return api_client.get(url)
