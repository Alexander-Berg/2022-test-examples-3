import pytest
from rest_framework import status
import urllib.request, urllib.parse, urllib.error

from email import message_from_string as parse_eml
from fan.message.render import RECIPIENT_EML_PLACEHOLDER, LETTER_SECRET_EML_PLACEHOLDER
from fan.testutils.matchers import assert_status_code, assert_validation_error
from fan.testutils.letter import get_html_body


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


def test_valid_eml(user_id, account, letter):
    response = do_request(account.name, letter.campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    assert_valid_eml_template(response.content.decode("utf-8"))


def test_missing_account_slug_param(campaign):
    response = do_request(None, campaign.slug)
    assert_validation_error(response, "account_slug", "not_found")


def test_missing_campaign_slug_param(account):
    response = do_request(account.name, None)
    assert_validation_error(response, "campaign_slug", "not_found")


def do_request(account_slug, campaign_slug):
    args = {
        "account_slug": account_slug,
        "campaign_slug": campaign_slug,
    }
    url = "/api/send/campaign-eml-template?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.get(url)


def assert_valid_eml_template(eml):
    parsed = parse_eml(eml)
    assert parsed["To"] == RECIPIENT_EML_PLACEHOLDER
    html_body = get_html_body(parsed)
    assert LETTER_SECRET_EML_PLACEHOLDER in html_body
