import pytest
from zlib import compress
import urllib.request, urllib.parse, urllib.error
from rest_framework import status
from fan.lists.singleuse import store_csv_maillist_for_campaign
from fan.testutils.matchers import (
    assert_invalid_emails_error,
    assert_status_code,
    assert_validation_error,
)
from fan.models.maillist import Maillist


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


def test_with_maillist(account, campaign_with_maillist):
    response = do_request(account.name, campaign_with_maillist.slug)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 2
    assert response_data == [
        {
            "email": "a@b.c",
            "user_template_params": {
                "user_variable_email": "a@b.c",
                "user_variable_name": "Иван",
                "user_variable_value": "100",
            },
        },
        {
            "email": "d@e.f",
            "user_template_params": {
                "user_variable_email": "d@e.f",
                "user_variable_name": "Марья",
                "user_variable_value": "50",
            },
        },
    ]


def test_with_singleusemaillist(account, campaign, maillist_csv_content):
    response = do_request(account.name, set_maillist(campaign, maillist_csv_content).slug)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 2
    assert response_data == [
        {
            "email": "a@b.c",
            "user_template_params": {
                "user_variable_email": "a@b.c",
                "user_variable_name": "Иван",
                "user_variable_value": "100",
            },
        },
        {
            "email": "d@e.f",
            "user_template_params": {
                "user_variable_email": "d@e.f",
                "user_variable_name": "Марья",
                "user_variable_value": "50",
            },
        },
    ]


def test_with_unicode_csv_maillist(account, campaign, maillist_content_with_unicode):
    response = do_request(account.name, set_maillist(campaign, maillist_content_with_unicode).slug)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 3
    assert response_data == [
        {
            "user_template_params": {
                "user_variable_имя": "",
                "user_variable_email": "a@b.ru",
                "user_variable_фамилия": "",
                "user_variable_компания": "",
                "user_variable_col5": "",
            },
            "email": "a@b.ru",
        },
        {
            "user_template_params": {
                "user_variable_имя": "Алена",
                "user_variable_email": "c@d.ru",
                "user_variable_фамилия": "Х",
                "user_variable_компания": "Ко",
                "user_variable_col5": "",
            },
            "email": "c@d.ru",
        },
        {
            "user_template_params": {
                "user_variable_имя": "",
                "user_variable_email": "e@f.ru",
                "user_variable_фамилия": "",
                "user_variable_компания": "",
                "user_variable_col5": "",
            },
            "email": "e@f.ru",
        },
    ]


def test_without_maillist(account, campaign):
    response = do_request(account.name, campaign.slug)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 0


def test_returns_stripped_emails(account, campaign, maillist_content_with_dirty_emails):
    set_maillist(campaign, maillist_content_with_dirty_emails)
    response = do_request(account.name, campaign.slug)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_data == [
        {
            "email": "a@b.c",
            "user_template_params": {
                "user_variable_email": "a@b.c",
            },
        },
        {
            "email": "a@b.c",
            "user_template_params": {
                "user_variable_email": "a@b.c",
            },
        },
    ]


def test_preserves_headers_schema(account, campaign, maillist_content_with_missed_values):
    response = do_request(
        account.name, set_maillist(campaign, maillist_content_with_missed_values).slug
    )
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_data == [
        {
            "email": "a@ya.ru",
            "user_template_params": {
                "user_variable_email": "a@ya.ru",
                "user_variable_a": "a1",
                "user_variable_b": "b1",
            },
        },
        {
            "email": "b@ya.ru",
            "user_template_params": {
                "user_variable_email": "b@ya.ru",
                "user_variable_a": "a2",
                "user_variable_b": "",
            },
        },
    ]


def test_hides_underscored_headers(account, campaign, maillist_content_with_underscored_header):
    response = do_request(
        account.name, set_maillist(campaign, maillist_content_with_underscored_header).slug
    )
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_data == [
        {
            "email": "a@ya.ru",
            "user_template_params": {
                "user_variable_email": "a@ya.ru",
                "user_variable_b": "b1",
            },
        },
    ]


def test_transforms_user_template_variables_to_lower(
    account, campaign, maillist_content_with_capital_letters_in_headers
):
    response = do_request(
        account.name, set_maillist(campaign, maillist_content_with_capital_letters_in_headers).slug
    )
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_data == [
        {
            "email": "a@ya.ru",
            "user_template_params": {
                "user_variable_email": "a@ya.ru",
                "user_variable_abc": "a1",
                "user_variable_cde": "b1",
            },
        },
    ]


def test_removes_duplicated_user_template_variables_in_different_case(
    account, campaign, maillist_content_with_duplicated_headers_in_different_case
):
    response = do_request(
        account.name,
        set_maillist(campaign, maillist_content_with_duplicated_headers_in_different_case).slug,
    )
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_data == [
        {
            "email": "a@ya.ru",
            "user_template_params": {
                "user_variable_email": "a@ya.ru",
                "user_variable_abc": "a1",
            },
        },
    ]


def test_removes_duplicated_user_template_variables(
    account, campaign, maillist_content_with_duplicated_headers
):
    response = do_request(
        account.name, set_maillist(campaign, maillist_content_with_duplicated_headers).slug
    )
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_data == [
        {
            "email": "a@ya.ru",
            "user_template_params": {
                "user_variable_email": "a@ya.ru",
                "user_variable_abc": "a1",
            },
        },
    ]


def test_fail_on_subscribers_limit_exceeded(account, campaign, too_large_maillist_content):
    force_set_maillist(campaign, too_large_maillist_content)
    response = do_request(account.name, campaign.slug)
    assert_validation_error(response, "data", "too_long")


def test_fail_on_variables_count_exceeded(account, campaign, csv_content_with_too_many_variables):
    force_set_maillist(campaign, csv_content_with_too_many_variables)
    response = do_request(account.name, campaign.slug)
    assert_validation_error(response, "data", "user_template_variables_count_exceeded")


def test_fail_on_too_long_variable(account, campaign, csv_content_with_too_long_variable):
    force_set_maillist(campaign, csv_content_with_too_long_variable)
    response = do_request(account.name, campaign.slug)
    assert_validation_error(response, "data", "user_template_variable_length_exceeded")


def test_fail_on_too_long_variable_value(
    account, campaign, csv_content_with_too_long_variable_value
):
    force_set_maillist(campaign, csv_content_with_too_long_variable_value)
    response = do_request(account.name, campaign.slug)
    assert_validation_error(response, "data", "user_template_variable_value_length_exceeded")


def test_fail_on_bad_csv(account, campaign, bad_csv_content):
    force_set_maillist(campaign, bad_csv_content)
    response = do_request(account.name, campaign.slug)
    assert_validation_error(response, "data", "email_column_not_found")


def test_fail_on_empty_csv(account, campaign, empty_csv_content):
    force_set_maillist(campaign, empty_csv_content)
    response = do_request(account.name, campaign.slug)
    assert_validation_error(response, "data", "empty")


def test_fail_on_invalid_email(account, campaign, corrupted_csv_content):
    force_set_maillist(campaign, corrupted_csv_content)
    response = do_request(account.name, campaign.slug)
    assert_invalid_emails_error(response, ["2a02:6b8:c08:d0a5:0:40b1:622b:eaa4"])


def do_request(account_slug, campaign_slug):
    args = {
        "account_slug": account_slug,
        "campaign_slug": campaign_slug,
    }
    url = "/api/send/campaign-recipient-list?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.get(url)


def set_maillist(campaign, content):
    store_csv_maillist_for_campaign(campaign, content, "singleusemaillist.csv")
    return campaign


def force_set_maillist(campaign, content):
    maillist = Maillist.objects.create(
        account=campaign.account,
        data=compress(content.encode("utf-8")),
        size=content.count("\n"),
    )
    campaign.maillist = maillist
    campaign.save()
