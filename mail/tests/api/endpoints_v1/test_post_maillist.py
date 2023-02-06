import pytest
from django.conf import settings
from urllib.parse import urlencode
from zlib import compress, decompress
from rest_framework import status
from rest_framework.test import APIClient
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_invalid_emails_error,
    assert_limit_reached_error,
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_wrong_state_error,
)
from fan.models import Campaign


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
def use_tvm_api_client_with_damaged_ticket(tvm_api_client_with_damaged_ticket):
    global client
    client = tvm_api_client_with_damaged_ticket
    yield
    client = None


def test_store_csv(user_id, account, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account.maillists.all()) == 1
    maillist = account.maillists.all()[0]
    assert maillist.slug
    assert "Список получателей" in maillist.title
    assert maillist.filename == "file.csv"
    assert decompress(maillist.data).decode() == maillist_csv_content
    assert maillist.preview == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]
    assert maillist.size == 2


def test_store_xls(user_id, account, maillist_xls_content, maillist_csv_content):
    response = do_request(user_id, account, maillist_xls_content, filename="file.xls")
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account.maillists.all()) == 1
    maillist = account.maillists.all()[0]
    assert maillist.slug
    assert "Список получателей" in maillist.title
    assert maillist.filename == "file.xls"
    assert decompress(maillist.data).decode() == maillist_csv_content
    assert maillist.preview == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]
    assert maillist.size == 2


def test_set_default_title_if_empty(user_id, account, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content, title="")
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account.maillists.all()) == 1
    maillist = account.maillists.all()[0]
    assert "Список получателей" in maillist.title


def test_set_title(user_id, account, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content, title="Название списка")
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account.maillists.all()) == 1
    maillist = account.maillists.all()[0]
    assert maillist.title == "Название списка"


def test_fail_on_duplicated_title(user_id, account, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content, title="Название списка")
    response = do_request(user_id, account, maillist_csv_content, title="Название списка")
    assert_validation_error(response, "title", "duplicated")


def test_fail_on_too_long_title(user_id, account, maillist_csv_content):
    response = do_request(
        user_id, account, maillist_csv_content, title="T" * (settings.MAILLIST_TITLE_MAX_LENGTH + 1)
    )
    assert_validation_error(response, "title", "too_long")


def test_response_json(user_id, account, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert response_data["slug"]
    assert response_data["filename"] == "file.csv"
    assert "Список получателей" in response_data["title"]
    assert response_data["preview"] == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]
    assert response_data["size"] == 2


def test_store_unicode_csv(user_id, account, maillist_content_with_unicode):
    response = do_request(user_id, account, maillist_content_with_unicode)
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account.maillists.all()) == 1
    maillist = account.maillists.all()[0]
    assert decompress(maillist.data).decode() == maillist_content_with_unicode


def test_store_two_maillists(user_id, account, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content)
    response = do_request(user_id, account, maillist_csv_content)
    assert_status_code(response, status.HTTP_200_OK)
    assert len(account.maillists.all()) == 2


def test_fail_storing_too_many_maillists(
    user_id, account_with_max_maillists_count, maillist_csv_content, overriden_maillists_limit
):
    response = do_request(user_id, account_with_max_maillists_count, maillist_csv_content)
    assert_limit_reached_error(response, "maillists_limit_reached")
    assert len(account_with_max_maillists_count.maillists.all()) == overriden_maillists_limit


def test_fail_wo_auth(user_id, account, maillist_csv_content, use_api_client_wo_auth):
    response = do_request(user_id, account, maillist_csv_content)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")
    assert len(account.maillists.all()) == 0


def test_fail_with_damaged_tvm_ticket(
    user_id, account, maillist_csv_content, use_tvm_api_client_with_damaged_ticket
):
    response = do_request(user_id, account, maillist_csv_content)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")
    assert len(account.maillists.all()) == 0


def test_fail_with_non_registered_tvm_source(
    user_id, account, maillist_csv_content, tvm_clear_api_v1_allowed_services_list
):
    response = do_request(user_id, account, maillist_csv_content)
    assert_forbidden_error(response, "forbidden_service")
    assert len(account.maillists.all()) == 0


def test_fail_with_forbidden_user(account, maillist_csv_content):
    response = do_request("forbidden_user_id", account, maillist_csv_content)
    assert_forbidden_error(response, "forbidden_user")


def test_fail_with_zip(user_id, account, maillist_csv_content):
    response = do_request(
        user_id,
        account,
        compress(maillist_csv_content.encode()),
        filename="file.zip",
        content_type="application/zip",
    )
    assert_validation_error(response, "data", "not_supported")
    assert len(account.maillists.all()) == 0


def test_fail_with_json(user_id, account):
    response = do_request(
        user_id,
        account,
        {"a": "b"},
        filename="file.json",
        content_type="application/json",
    )
    assert_validation_error(response, "data", "not_supported")
    assert len(account.maillists.all()) == 0


def test_fail_wo_filename(user_id, account, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content, filename="")
    assert_validation_error(response, "filename", "empty")


def test_fail_with_unexisted_account(user_id, account, maillist_csv_content):
    account.name = "invalid"
    response = do_request(user_id, account, maillist_csv_content)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_fail_with_bad_csv(user_id, account, bad_csv_content):
    response = do_request(user_id, account, bad_csv_content)
    assert_validation_error(response, "data", "email_column_not_found")


def test_fail_with_empty_csv(user_id, account, empty_csv_content):
    response = do_request(user_id, account, empty_csv_content)
    assert_validation_error(response, "data", "empty")


def test_fail_with_corrupted_csv(user_id, account, corrupted_csv_content):
    response = do_request(user_id, account, corrupted_csv_content)
    assert_invalid_emails_error(response, ["2a02:6b8:c08:d0a5:0:40b1:622b:eaa4"])


def test_allow_empty_email(user_id, account, missed_email_csv_content):
    response = do_request(user_id, account, missed_email_csv_content)
    assert_status_code(response, status.HTTP_200_OK)


def test_fail_on_subscribers_limit_exceeded(user_id, account, too_large_maillist_content):
    response = do_request(user_id, account, too_large_maillist_content)
    assert_validation_error(response, "data", "too_long")


def test_fail_on_variables_count_exceeded(user_id, account, csv_content_with_too_many_variables):
    response = do_request(user_id, account, csv_content_with_too_many_variables)
    assert_validation_error(response, "data", "user_template_variables_count_exceeded")


def test_do_not_count_hidden_variables(
    user_id, account, csv_content_with_too_many_variables_but_some_are_hidden_variable
):
    response = do_request(
        user_id, account, csv_content_with_too_many_variables_but_some_are_hidden_variable
    )
    assert_status_code(response, status.HTTP_200_OK)


def test_fail_with_too_long_variable(user_id, account, csv_content_with_too_long_variable):
    response = do_request(user_id, account, csv_content_with_too_long_variable)
    assert_validation_error(response, "data", "user_template_variable_length_exceeded")


def test_ignore_too_long_hidden_variable(
    user_id, account, csv_content_with_too_long_hidden_variable
):
    response = do_request(user_id, account, csv_content_with_too_long_hidden_variable)
    assert_status_code(response, status.HTTP_200_OK)


def test_fail_with_too_long_variable_value(
    user_id, account, csv_content_with_too_long_variable_value
):
    response = do_request(user_id, account, csv_content_with_too_long_variable_value)
    assert_validation_error(response, "data", "user_template_variable_value_length_exceeded")


def test_ignore_too_long_hidden_variable_value(
    user_id, account, csv_content_with_too_long_hidden_variable_value
):
    response = do_request(user_id, account, csv_content_with_too_long_hidden_variable_value)
    assert_status_code(response, status.HTTP_200_OK)


def test_set_maillist_for_draft_campaign(user_id, account, campaign, maillist_csv_content):
    response = do_request(user_id, account, maillist_csv_content, campaign=campaign)
    assert_status_code(response, status.HTTP_200_OK)
    campaign.refresh_from_db()
    assert campaign.maillist is not None
    assert decompress(campaign.maillist.data).decode() == maillist_csv_content


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_fail_on_setting_maillist_for_wrong_state_campaign(
    user_id, account, campaign, maillist_csv_content, wrong_state
):
    force_set_campaign_state(campaign, wrong_state)
    response = do_request(user_id, account, maillist_csv_content, campaign=campaign)
    assert_wrong_state_error(response, wrong_state, "draft")


def test_fail_on_setting_maillist_for_unexisted_campaign(
    user_id, account, campaign, maillist_csv_content
):
    campaign.slug = "unexisted"
    response = do_request(user_id, account, maillist_csv_content, campaign=campaign)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def do_request(
    user_id,
    account,
    data,
    filename="file.csv",
    content_type="application/octet-stream",
    campaign=None,
    title=None,
):
    query_params = {
        "user_id": user_id,
        "account_slug": account.name,
        "filename": filename,
        "campaign_slug": campaign.slug if campaign else campaign,
        "title": title,
    }
    url = "/api/v1/maillist?" + urlencode(
        {k: v for k, v in list(query_params.items()) if v is not None}
    )
    global client
    return client.post(path=url, data=data, content_type=content_type)


def force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
