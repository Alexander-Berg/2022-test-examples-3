import pytest
import zlib

from django.conf import settings
from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_wrong_state_error,
    assert_forbidden_error,
    assert_invalid_emails_error,
)
from fan.models import Campaign
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


def test_put_csv_stores_successfuly(user_id, account, campaign, maillist_csv_content):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.maillist_uploaded is True


def test_put_xls_stores_successfuly(user_id, account, campaign, maillist_xls_content):
    response = do_request(user_id, account, campaign, maillist_xls_content, filename="file.xls")
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.maillist_uploaded is True


def test_put_unicode_csv_stores_successfuly(
    user_id, account, campaign, maillist_content_with_unicode
):
    response = do_request(user_id, account, campaign, maillist_content_with_unicode)
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.maillist_uploaded is True
    maillist = campaign.single_use_maillist
    assert zlib.decompress(maillist.data).decode() == maillist_content_with_unicode
    assert maillist.description == "file.csv"


def test_unsets_maillist(user_id, account, campaign_with_maillist, maillist_csv_content):
    response = do_request(user_id, account, campaign_with_maillist, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    campaign_with_maillist.refresh_from_db()
    assert campaign_with_maillist.maillist == None
    assert campaign_with_maillist.maillist_uploaded == True
    assert campaign_with_maillist.maillist_description == "file.csv"


def test_does_not_delete_maillist(user_id, account, campaign_with_maillist, maillist_csv_content):
    response = do_request(user_id, account, campaign_with_maillist, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    assert len(campaign_with_maillist.account.maillists.all()) == 1


def test_put_csv_stores_successfuly_session_auth(
    user_id, account, campaign, maillist_csv_content, use_session_auth_api_client
):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.maillist_uploaded is True


def test_put_csv_stores_successfuly_wo_auth_localhost(
    user_id,
    account,
    campaign,
    maillist_csv_content,
    use_api_client_wo_auth,
    disable_auth_on_loopback,
):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.maillist_uploaded is True


def test_put_csv_wo_auth(user_id, account, campaign, maillist_csv_content, use_api_client_wo_auth):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_put_csv_with_damaged_tvm_credentials(
    user_id, account, campaign, maillist_csv_content, use_tvm_api_client_with_damaged_ticket
):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_put_csv_with_non_registered_tvm_source(
    user_id, account, campaign, maillist_csv_content, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_forbidden_error(response, "forbidden_service")


def test_put_csv_stores_correct_data(user_id, account, campaign, maillist_csv_content):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    maillist = campaign.single_use_maillist
    assert zlib.decompress(maillist.data).decode() == maillist_csv_content
    assert maillist.description == "file.csv"


def test_put_rejects_zip(user_id, account, campaign, maillist_csv_content):
    response = do_request(
        user_id,
        account,
        campaign,
        data=zlib.compress(maillist_csv_content.encode()),
        filename="file.zip",
        content_type="application/zip",
    )
    assert_validation_error(response, "data", "not_supported")


def test_put_csv_twice_returns_200(user_id, account, campaign, maillist_csv_content):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)


def test_put_csv_twice_keeps_last_filename(user_id, account, campaign, maillist_csv_content):
    response = do_request(user_id, account, campaign, maillist_csv_content, filename="file1.csv")
    response = do_request(user_id, account, campaign, maillist_csv_content, filename="file2.csv")
    maillist = campaign.single_use_maillist
    assert_status_code(response, status.HTTP_200_OK)
    assert maillist.description == "file2.csv"


def test_put_json_instead_csv_returns_400(user_id, account, campaign):
    response = do_request(
        user_id,
        account,
        campaign,
        data={"a": "b"},
        filename="file.json",
        content_type="application/json",
    )
    assert_validation_error(response, "data", "not_supported")


def test_put_csv_without_filename_returns_400(user_id, account, campaign, maillist_csv_content):
    response = do_request(user_id, account, campaign, maillist_csv_content, filename="")
    assert_validation_error(response, "filename", "empty")


def test_put_csv_with_invalid_account_returns_404(user_id, account, campaign, maillist_csv_content):
    account.name = "invalid"
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_put_csv_with_invalid_campaign_returns_404(
    user_id, account, campaign, maillist_csv_content
):
    campaign.slug = "invalid"
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_put_csv_fails_since_reach_limit(user_id, account, campaign, maillist_csv_content):
    lines = maillist_csv_content.splitlines()
    before_limit = settings.MAX_MAILLIST_RECIPIENTS - len(lines)
    padding = [lines[-1]] * (before_limit + 100)
    data = "\n".join(lines + padding)
    response = do_request(user_id, account, campaign, data)
    assert_validation_error(response, "data", "too_long")


def test_put_bad_csv_fails(user_id, account, campaign, bad_csv_content):
    response = do_request(user_id, account, campaign, bad_csv_content)
    assert_validation_error(response, "data", "email_column_not_found")


def test_put_empty_csv_fails(user_id, account, campaign, empty_csv_content):
    response = do_request(user_id, account, campaign, empty_csv_content)
    assert_validation_error(response, "data", "empty")


def test_put_csv_too_many_variables_fails(
    user_id, account, campaign, csv_content_with_too_many_variables
):
    response = do_request(user_id, account, campaign, csv_content_with_too_many_variables)
    assert_validation_error(response, "data", "user_template_variables_count_exceeded")


def test_do_not_count_hidden_variables(
    user_id, account, campaign, csv_content_with_too_many_variables_but_some_are_hidden_variable
):
    response = do_request(
        user_id, account, campaign, csv_content_with_too_many_variables_but_some_are_hidden_variable
    )
    assert_status_code(response, status.HTTP_200_OK)


def test_put_csv_with_too_long_variable_fails(
    user_id, account, campaign, csv_content_with_too_long_variable
):
    response = do_request(user_id, account, campaign, csv_content_with_too_long_variable)
    assert_validation_error(response, "data", "user_template_variable_length_exceeded")


def test_ignore_too_long_hidden_variable(
    user_id, account, campaign, csv_content_with_too_long_hidden_variable
):
    response = do_request(user_id, account, campaign, csv_content_with_too_long_hidden_variable)
    assert_status_code(response, status.HTTP_200_OK)


def test_put_csv_with_too_long_variable_value_fails(
    user_id, account, campaign, csv_content_with_too_long_variable_value
):
    response = do_request(user_id, account, campaign, csv_content_with_too_long_variable_value)
    assert_validation_error(response, "data", "user_template_variable_value_length_exceeded")


def test_ignore_too_long_hidden_variable_value(
    user_id, account, campaign, csv_content_with_too_long_hidden_variable_value
):
    response = do_request(
        user_id, account, campaign, csv_content_with_too_long_hidden_variable_value
    )
    assert_status_code(response, status.HTTP_200_OK)


def test_put_corrupted_csv_fails(user_id, account, campaign, corrupted_csv_content):
    response = do_request(user_id, account, campaign, corrupted_csv_content)
    assert_invalid_emails_error(response, ["2a02:6b8:c08:d0a5:0:40b1:622b:eaa4"])


def test_allow_putting_csv_with_empty_email(user_id, account, campaign, missed_email_csv_content):
    response = do_request(user_id, account, campaign, missed_email_csv_content)
    assert_status_code(response, status.HTTP_200_OK)


def test_maillist_summary_response(user_id, account, campaign, maillist_csv_content):
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["uploaded"] == True
    assert response_data["description"] == "file.csv"
    assert response_data["preview"] == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_putting_fails_on_wrong_campaign_state(
    user_id, account, campaign, maillist_csv_content, wrong_state
):
    _force_set_campaign_state(campaign, wrong_state)
    response = do_request(user_id, account, campaign, maillist_csv_content)
    assert_wrong_state_error(response, wrong_state, "draft")


def test_forbidden_user(account, campaign, maillist_csv_content):
    response = do_request("forbidden_user_id", account, campaign, maillist_csv_content)
    assert_forbidden_error(response, "forbidden_user")


def do_request(
    user_id, account, campaign, data, filename="file.csv", content_type="application/octet-stream"
):
    url = "/api/v1/campaign-maillist?filename={filename}&user_id={user_id}&account_slug={account_slug}&campaign_slug={campaign_slug}".format(
        filename=filename,
        user_id=user_id,
        account_slug=account.name,
        campaign_slug=campaign.slug,
    )
    global client
    return client.put(path=url, data=data, content_type=content_type)


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
