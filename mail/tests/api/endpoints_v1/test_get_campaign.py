import pytest
import urllib.request, urllib.parse, urllib.error

from fan.message.get import get_message_by_letter
from fan.message.letter import load_letter
from fan.models import Campaign
from fan.testutils.campaign import set_delivery_stats
from fan.testutils.letter import load_test_letter
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    is_subdict,
)
from fan.testutils.utils import format_nullable_datetime
from rest_framework import status
from rest_framework.test import APIClient


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
def use_session_auth_api_client(auth_api_client):
    global client
    client = auth_api_client
    yield
    client = None


@pytest.fixture
def use_tvm_api_client_with_damaged_ticket(tvm_api_client_with_damaged_ticket):
    global client
    client = tvm_api_client_with_damaged_ticket
    yield
    client = None


@pytest.fixture
def letter_with_variable_in_different_case_html():
    letter_file = load_test_letter("letter_with_variable_in_different_case.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def campaign_with_letter_with_variable_in_different_case(
    campaign_with_letter, letter_with_variable_in_different_case_html
):
    load_letter(campaign_with_letter.default_letter, letter_with_variable_in_different_case_html)
    yield campaign_with_letter


@pytest.fixture
def campaign_stats_without_feedback(campaign_stats):
    campaign_stats["unsubscribed_after"] = 0
    campaign_stats["views"] = 0
    return campaign_stats


@pytest.fixture
def sent_campaign_without_feedback(sending_campaign, campaign_stats_without_feedback):
    sending_campaign.state = Campaign.STATUS_SENT
    sending_campaign.save()
    set_delivery_stats(sending_campaign, campaign_stats_without_feedback)
    return sending_campaign


def test_campaign(user_id, account, campaign):
    response = do_request(user_id, account.name, campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data == campaign_to_dict(campaign)


def test_response_for_campaign_with_maillist(user_id, account, campaign_with_maillist):
    response = do_request(user_id, account.name, campaign_with_maillist.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(
        {
            "state": Campaign.STATUS_DRAFT,
            "from_email": "",
            "from_name": "",
            "subject": "",
            "maillist_uploaded": True,
            "maillist_description": "maillist.csv",
            "maillist_preview": [
                {"email": "a@b.c", "name": "Иван", "value": "100"},
                {"email": "d@e.f", "name": "Марья", "value": "50"},
            ],
            "maillist_size": 2,
            "maillist_slug": campaign_with_maillist.maillist.slug,
            "maillist_title": "Список рассылки 0",
            "maillist_created_at": format_nullable_datetime(
                campaign_with_maillist.maillist_created_at
            ),
            "maillist_modified_at": format_nullable_datetime(
                campaign_with_maillist.maillist_modified_at
            ),
            "letter_uploaded": False,
            "letter_description": "",
            "letter_user_template_variables": [],
        },
        response_data,
    )


def test_campaign_session_auth(use_session_auth_api_client, user_id, account, campaign):
    response = do_request(user_id, account.name, campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data == campaign_to_dict(campaign)


def test_campaign_wo_auth_localhost(
    use_api_client_wo_auth, user_id, account, campaign, disable_auth_on_loopback
):
    response = do_request(user_id, account.name, campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data == campaign_to_dict(campaign)


def test_campaign_wo_auth(use_api_client_wo_auth, user_id, account, campaign):
    response = do_request(user_id, account.name, campaign.slug)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_campaign_with_damaged_tvm_credentials(
    use_tvm_api_client_with_damaged_ticket, user_id, account, campaign
):
    response = do_request(user_id, account.name, campaign.slug)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_campaign_with_non_registered_tvm_source(
    user_id, account, campaign, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account.name, campaign.slug)
    assert_forbidden_error(response, "forbidden_service")


def test_campaign_missing_account_slug(user_id, campaign):
    response = do_request(user_id, None, campaign.slug)
    assert_validation_error(response, "account_slug", "not_found")


def test_campaign_missing_campaign_slug(user_id, account):
    response = do_request(user_id, account.name, None)
    assert_validation_error(response, "campaign_slug", "not_found")


def test_campaign_non_existent_account(user_id, campaign):
    response = do_request(user_id, "NONEXISTENT", campaign.slug)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_campaign_non_existent_campaign(user_id, account):
    response = do_request(user_id, account.name, "NONEXISTENT")
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_forbidden_user(account, campaign):
    response = do_request("forbidden_user_id", account.name, campaign.slug)
    assert_forbidden_error(response, "forbidden_user")


def test_null_stats(user_id, account, campaign):
    response = do_request(user_id, account.name, campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["stats"] == None


def test_sent_campaign_without_feedback(
    user_id, account, sent_campaign_without_feedback, campaign_stats_without_feedback
):
    response = do_request(user_id, account.name, sent_campaign_without_feedback.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["stats"] == campaign_stats_without_feedback


def test_stats(user_id, account, sent_campaign, campaign_stats):
    response = do_request(user_id, account.name, sent_campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["stats"] == campaign_stats


def test_contains_letter_user_template_variables_on_campaign_with_letter(
    user_id, account, campaign_with_letter
):
    response = do_request(user_id, account.name, campaign_with_letter.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert "letter_user_template_variables" in response_data
    assert response_data["letter_user_template_variables"] == [
        "name"
    ]  # doesn't count variables in subject


def test_contains_letter_user_template_variables_in_lower_case(
    user_id, account, campaign_with_letter_with_variable_in_different_case
):
    response = do_request(
        user_id, account.name, campaign_with_letter_with_variable_in_different_case.slug
    )
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert "letter_user_template_variables" in response_data
    assert response_data["letter_user_template_variables"] == ["var"]


def test_contains_empty_letter_user_template_variables_on_campaign_without_letter(
    user_id, account, campaign
):
    response = do_request(user_id, account.name, campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert "letter_user_template_variables" in response_data
    assert len(response_data["letter_user_template_variables"]) == 0


def test_contains_maillist_preview_on_campaign_with_singleusemaillist(
    user_id, account, campaign_with_singleusemaillist
):
    response = do_request(user_id, account.name, campaign_with_singleusemaillist.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["maillist_preview"] == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]


def test_maillist_fields_on_campaign_with_singleusemaillist(
    user_id, account, campaign_with_singleusemaillist
):
    response = do_request(user_id, account.name, campaign_with_singleusemaillist.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    single_use_maillist = campaign_with_singleusemaillist.single_use_maillist
    assert response_data["maillist_uploaded"]
    assert response_data["maillist_description"] == single_use_maillist.description
    assert response_data["maillist_size"] == single_use_maillist.subscribers_number
    assert response_data["maillist_created_at"] == format_nullable_datetime(
        single_use_maillist.created_at
    )
    assert response_data["maillist_modified_at"] == format_nullable_datetime(
        single_use_maillist.modified_at
    )


def test_contains_maillist_preview_on_campaign_without_maillist(user_id, account, campaign):
    response = do_request(user_id, account.name, campaign.slug)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["maillist_preview"] == []


def do_request(user_id, account_slug, campaign_slug):
    query_params = {}
    if user_id is not None:
        query_params["user_id"] = user_id
    if account_slug is not None:
        query_params["account_slug"] = account_slug
    if campaign_slug is not None:
        query_params["campaign_slug"] = campaign_slug
    url = "/api/v1/campaign?" + urllib.parse.urlencode(query_params)
    global client
    return client.get(url)


def campaign_to_dict(campaign):
    message = get_message_by_letter(campaign.default_letter)
    letter_user_template_variables = message.user_template_variables
    return {
        "created_at": format_nullable_datetime(campaign.created_at),
        "created_by": campaign.created_by,
        "from_email": campaign.from_email,
        "from_name": campaign.from_name,
        "modified_at": format_nullable_datetime(campaign.modified_at),
        "slug": campaign.slug,
        "state": campaign.state,
        "subject": campaign.subject,
        "title": campaign.title,
        "letter_uploaded": campaign.letter_uploaded,
        "letter_description": campaign.letter_description,
        "letter_user_template_variables": letter_user_template_variables,
        "maillist_uploaded": campaign.maillist_uploaded,
        "maillist_description": campaign.maillist_description,
        "maillist_preview": campaign.maillist_preview,
        "maillist_size": campaign.estimated_subscribers_number(),
        "maillist_slug": "",
        "maillist_title": "",
        "maillist_created_at": "",
        "maillist_modified_at": "",
        "stats": campaign.stats,
    }
