import pytest
from fan.models import Campaign
from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_wrong_state_error,
    assert_forbidden_error,
    is_subdict,
)
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
def campaign_details():
    return {
        "from_email": "test@from.email",
        "from_name": "test_from_name",
        "subject": "test_subject",
    }


def test_all_campaign_details(user_id, account, campaign, campaign_details):
    response = do_request(user_id, account, campaign, campaign_details)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(campaign_details, response_data)
    assert response_data["title"] == campaign_details["subject"]


def test_response_for_campaign_with_maillist(
    user_id, account, campaign_with_maillist, campaign_details
):
    response = do_request(user_id, account, campaign_with_maillist, campaign_details)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(
        {
            "state": Campaign.STATUS_DRAFT,
            "from_email": campaign_details["from_email"],
            "from_name": campaign_details["from_name"],
            "subject": campaign_details["subject"],
            "maillist_uploaded": True,
            "maillist_description": "maillist.csv",
            "maillist_preview": [
                {"email": "a@b.c", "name": "Иван", "value": "100"},
                {"email": "d@e.f", "name": "Марья", "value": "50"},
            ],
            "maillist_size": 2,
            "maillist_slug": campaign_with_maillist.maillist.slug,
            "maillist_title": "Список рассылки 0",
            "letter_uploaded": False,
            "letter_description": "",
            "letter_user_template_variables": [],
        },
        response_data,
    )


def test_all_campaign_details_sessions_auth(
    use_session_auth_api_client, user_id, account, campaign, campaign_details
):
    response = do_request(user_id, account, campaign, campaign_details)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(campaign_details, response_data)
    assert response_data["title"] == campaign_details["subject"]


def test_all_campaign_details_wo_auth_localhost(
    use_api_client_wo_auth, user_id, account, campaign, campaign_details, disable_auth_on_loopback
):
    response = do_request(user_id, account, campaign, campaign_details)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(campaign_details, response_data)
    assert response_data["title"] == campaign_details["subject"]


def test_all_campaign_details_wo_auth(
    use_api_client_wo_auth, user_id, account, campaign, campaign_details
):
    response = do_request(user_id, account, campaign, campaign_details)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_all_campaign_details_with_damaged_tvm_credentials(
    use_tvm_api_client_with_damaged_ticket, user_id, account, campaign, campaign_details
):
    response = do_request(user_id, account, campaign, campaign_details)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_all_campaign_details_with_non_registered_tvm_source(
    user_id, account, campaign, campaign_details, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account, campaign, campaign_details)
    assert_forbidden_error(response, "forbidden_service")


def test_some_campaign_details(user_id, account, campaign, campaign_details):
    del campaign_details["from_email"]
    response = do_request(user_id, account, campaign, campaign_details)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(campaign_details, response_data)
    assert response_data["title"] == campaign_details["subject"]


def test_one_campaign_detail(user_id, account, campaign, campaign_details):
    del campaign_details["from_email"]
    del campaign_details["from_name"]
    response = do_request(user_id, account, campaign, campaign_details)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert is_subdict(campaign_details, response_data)
    assert response_data["title"] == campaign_details["subject"]


def test_empty_campaign_details(user_id, account, campaign):
    response = do_request(user_id, account, campaign, {})
    assert_validation_error(response, "non_field", "empty_params")


def test_empty_body(user_id, account, campaign):
    response = do_request(user_id, account, campaign, None)
    assert_validation_error(response, "non_field", "empty_params")


def test_list_instead_of_object(user_id, account, campaign):
    response = do_request(user_id, account, campaign, [])
    assert_validation_error(response, "non_field", "invalid_type")


def test_unexistent_campaign_detail(user_id, account, campaign, campaign_details):
    campaign_details["unexistent"] = "value"
    response = do_request(user_id, account, campaign, campaign_details)
    assert_validation_error(response, "unexistent", "not_supported")


def test_wrong_type_campaign_detail(user_id, account, campaign, campaign_details):
    campaign_details["subject"] = 123
    response = do_request(user_id, account, campaign, campaign_details)
    assert_validation_error(response, "subject", "invalid_type")


def test_wrong_email(user_id, account, campaign, campaign_details):
    campaign_details["from_email"] = "wrong_email"
    response = do_request(user_id, account, campaign, campaign_details)
    assert_validation_error(response, "from_email", "invalid_email")


def test_too_long_subject(user_id, account, campaign, campaign_details):
    campaign_details["subject"] = 1025 * "a"
    response = do_request(user_id, account, campaign, campaign_details)
    assert_validation_error(response, "subject", "too_long")


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_putting_fails_on_wrong_campaign_state(
    user_id, account, campaign, campaign_details, wrong_state
):
    force_set_campaign_state(campaign, wrong_state)
    response = do_request(user_id, account, campaign, campaign_details)
    assert_wrong_state_error(response, wrong_state, "draft")


def test_forbidden_user(account, campaign, campaign_details):
    response = do_request("forbidden_user_id", account, campaign, campaign_details)
    assert_forbidden_error(response, "forbidden_user")


def do_request(user_id, account, campaign, campaign_details):
    url = "/api/v1/campaign-details?user_id={user_id}&account_slug={account_slug}&campaign_slug={campaign_slug}".format(
        user_id=user_id, account_slug=account.name, campaign_slug=campaign.slug
    )
    global client
    return client.put(url, campaign_details, format="json")


def force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
