import pytest
from urllib.parse import urlencode
from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_wrong_state_error,
    assert_forbidden_error,
)
from fan.models import Account, Campaign, Maillist, SingleUseMailList
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


def test_sets_maillist_for_campaign(user_id, account, campaign, maillist):
    response = do_request(user_id, account, campaign, maillist)
    campaign.refresh_from_db()
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.maillist == maillist


def test_overwrites_singleusemaillist(user_id, account, campaign_with_singleusemaillist, maillist):
    response = do_request(user_id, account, campaign_with_singleusemaillist, maillist)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["maillist_uploaded"] == True
    assert response_data["maillist_description"] == "maillist.csv"


def test_deletes_singleusemaillist(user_id, account, campaign_with_singleusemaillist, maillist):
    response = do_request(user_id, account, campaign_with_singleusemaillist, maillist)
    assert_status_code(response, status.HTTP_200_OK)
    assert len(SingleUseMailList.objects.all()) == 0


def test_response(user_id, account, campaign, maillist):
    response = do_request(user_id, account, campaign, maillist)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["maillist_uploaded"] == True
    assert response_data["maillist_description"] == "maillist.csv"
    assert response_data["maillist_preview"] == [
        {"value": "100", "email": "a@b.c", "name": "Иван"},
        {"value": "50", "email": "d@e.f", "name": "Марья"},
    ]
    assert response_data["maillist_size"] == 2
    assert response_data["maillist_slug"] == maillist.slug
    assert response_data["maillist_title"] == "Список рассылки 0"


def test_wo_auth(user_id, account, campaign, maillist, use_api_client_wo_auth):
    response = do_request(user_id, account, campaign, maillist)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_with_damaged_tvm_credentials(
    user_id, account, campaign, maillist, use_tvm_api_client_with_damaged_ticket
):
    response = do_request(user_id, account, campaign, maillist)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_with_non_registered_tvm_source(
    user_id, account, campaign, maillist, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account, campaign, maillist)
    assert_forbidden_error(response, "forbidden_service")


def test_put_twice_returns_200(user_id, account, campaign, maillist):
    response = do_request(user_id, account, campaign, maillist)
    response = do_request(user_id, account, campaign, maillist)
    assert_status_code(response, status.HTTP_200_OK)


def test_put_twice_keeps_last_maillist(user_id, account, campaign, maillist, another_maillist):
    response = do_request(user_id, account, campaign, maillist)
    response = do_request(user_id, account, campaign, another_maillist)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["maillist_uploaded"] == True
    assert response_data["maillist_description"] == "another_maillist.csv"


def test_with_invalid_account(user_id, account, campaign, maillist):
    account.name = "invalid"
    response = do_request(user_id, account, campaign, maillist)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_with_invalid_campaign(user_id, account, campaign, maillist):
    campaign.slug = "invalid"
    response = do_request(user_id, account, campaign, maillist)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_fails_on_wrong_campaign_state(user_id, account, campaign, maillist, wrong_state):
    _force_set_campaign_state(campaign, wrong_state)
    response = do_request(user_id, account, campaign, maillist)
    assert_wrong_state_error(response, wrong_state, "draft")


def test_forbidden_user(account, campaign, maillist):
    response = do_request("forbidden_user_id", account, campaign, maillist)
    assert_forbidden_error(response, "forbidden_user")


def do_request(user_id, account, campaign, maillist):
    query_params = {
        "user_id": user_id,
        "account_slug": account.name if isinstance(account, Account) else account,
        "campaign_slug": campaign.slug if isinstance(campaign, Campaign) else campaign,
        "maillist_slug": maillist.slug if isinstance(maillist, Maillist) else maillist,
    }
    url = "/api/v1/campaign-maillist?" + urlencode(
        {k: v for k, v in list(query_params.items()) if v is not None}
    )
    global client
    return client.put(path=url)


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
