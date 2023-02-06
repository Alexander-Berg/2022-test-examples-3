import pytest
from django.conf import settings
from fan.testutils.matchers import (
    assert_status_code,
    assert_validation_error,
    assert_forbidden_error,
    assert_limit_reached_error,
    is_subdict,
)
from fan.models import Campaign
from rest_framework import status


pytestmark = pytest.mark.django_db


@pytest.fixture()
def mock_draft_campaigns_limit():
    saved = settings.DRAFT_CAMPAIGNS_LIMIT
    settings.DRAFT_CAMPAIGNS_LIMIT = 2
    yield
    settings.DRAFT_CAMPAIGNS_LIMIT = saved


def test_create_campaign_missing_account_slug(auth_api_client, user_id):
    url = "/api/v1/campaign?user_id={user_id}".format(
        user_id=user_id,
    )
    response = auth_api_client.post(url)
    assert_validation_error(response, "account_slug", "not_found")


def test_create_campaign_missing_user_id(auth_api_client, account):
    url = "/api/v1/campaign?account_slug={account_slug}".format(
        account_slug=account.name,
    )
    response = auth_api_client.post(url)
    assert_validation_error(response, "user_id", "not_found")


def test_create_campaign_non_existent_account(auth_api_client, user_id):
    url = "/api/v1/campaign?user_id={user_id}&account_slug={account_slug}".format(
        user_id=user_id,
        account_slug="non-existing-account",
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_clone_campaign_non_existent_source_campaign(auth_api_client, user_id, account):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug="non-existent-source-campaign",
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_create_campaign(auth_api_client, user_id, account):
    url = "/api/v1/campaign?user_id={user_id}&account_slug={account_slug}".format(
        user_id=user_id,
        account_slug=account.name,
    )
    expected_campaign = {
        "created_by": user_id,
        "from_email": "",
        "from_name": "",
        "state": "draft",
        "subject": "",
    }
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(expected_campaign, response_data)


def test_draft_campaigns_limit_on_create_campaign(
    mock_draft_campaigns_limit, auth_api_client, user_id, account, campaign
):
    url = "/api/v1/campaign?user_id={user_id}&account_slug={account_slug}".format(
        user_id=user_id,
        account_slug=account.name,
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response = auth_api_client.post(url)
    assert_limit_reached_error(response, "draft_campaigns_limit_reached")


def test_draft_campaigns_limit_on_clone_campaign(
    mock_draft_campaigns_limit, auth_api_client, user_id, account, campaign
):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug=campaign.slug,
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response = auth_api_client.post(url)
    assert_limit_reached_error(response, "draft_campaigns_limit_reached")


def test_clone_empty_campaign(auth_api_client, user_id, account, campaign):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug=campaign.slug,
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"] != campaign.slug
    assert response_data["created_by"] == user_id
    assert response_data["from_email"] == ""
    assert response_data["from_name"] == ""
    assert response_data["subject"] == ""
    assert response_data["state"] == "draft"
    assert response_data["maillist_uploaded"] == False
    assert response_data["letter_uploaded"] == False


def test_clone_campaign_with_letter(auth_api_client, user_id, account, campaign_with_letter):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug=campaign_with_letter.slug,
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"] != campaign_with_letter.slug
    assert response_data["created_by"] == user_id
    assert response_data["from_email"] == campaign_with_letter.from_email
    assert response_data["from_name"] == campaign_with_letter.from_name
    assert response_data["subject"] == campaign_with_letter.subject
    assert response_data["state"] == "draft"
    assert response_data["maillist_uploaded"] == False
    assert response_data["letter_uploaded"] == True


def test_clone_campaign_with_singleusemaillist(
    auth_api_client, user_id, account, campaign_with_singleusemaillist
):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug=campaign_with_singleusemaillist.slug,
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"] != campaign_with_singleusemaillist.slug
    assert response_data["created_by"] == user_id
    assert response_data["state"] == "draft"
    assert response_data["maillist_uploaded"] == True
    assert response_data["letter_uploaded"] == False


def test_clone_ready_campaign(auth_api_client, user_id, account, ready_campaign):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug=ready_campaign.slug,
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"] != ready_campaign.slug
    assert response_data["created_by"] == user_id
    assert response_data["state"] == "draft"
    assert response_data["maillist_uploaded"] == True
    assert response_data["letter_uploaded"] == True


def test_clone_sent_campaign(auth_api_client, user_id, account, sent_campaign):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug=sent_campaign.slug,
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"] != sent_campaign.slug
    assert response_data["created_by"] == user_id
    assert response_data["state"] == "draft"
    assert response_data["maillist_uploaded"] == True
    assert response_data["letter_uploaded"] == True


def test_cloning_sets_new_created_at(auth_api_client, user_id, account, campaign):
    url = (
        "/api/v1/campaign?"
        "user_id={user_id}&"
        "account_slug={account_slug}&"
        "source_campaign_slug={source_campaign_slug}".format(
            user_id=user_id,
            account_slug=account.name,
            source_campaign_slug=campaign.slug,
        )
    )
    response = auth_api_client.post(url)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    created_campaign = Campaign.objects.get(slug=response_data["slug"])
    assert created_campaign.created_at > campaign.created_at


def test_forbidden_user(auth_api_client, account):
    url = "/api/v1/campaign?user_id={user_id}&account_slug={account_slug}".format(
        user_id="forbidden_user_id",
        account_slug=account.name,
    )
    response = auth_api_client.post(url)
    assert_forbidden_error(response, "forbidden_user")
