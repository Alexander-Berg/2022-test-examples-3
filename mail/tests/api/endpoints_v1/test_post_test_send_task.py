import pytest
from django.conf import settings
from rest_framework import status
from rest_framework.test import APIClient
from fan.campaigns.set import set_campaign_details
from fan.models import TestSendTask as Task
from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_wrong_domain_error,
    assert_wrong_state_error,
    assert_not_ready_error,
    assert_forbidden_error,
    assert_limit_reached_error,
    assert_wrong_login_error,
    assert_invalid_emails_error,
)


pytestmark = pytest.mark.django_db


@pytest.fixture
def recipients():
    return ["recipient1@yandex.ru", "recipient2@yandex.ru"]


@pytest.fixture
def many_recipients():
    return ["recipient{}@yandex.ru".format(i) for i in range(20)]


@pytest.fixture
def correct_user_template_variables():
    return {"name": "Someone"}


@pytest.fixture
def correct_user_template_variables_with_capital_letters():
    return {"nAmE": "Someone"}


@pytest.fixture
def duplicated_user_template_variables_in_different_cases():
    return {"nAmE": "Someone1", "NaMe": "Someone2"}


@pytest.fixture
def wrong_type_user_template_variables():
    return [("name", "Someone"), ("title", "Something")]


@pytest.fixture
def wrong_type_user_template_variable_value():
    return {"name": 12345}


@pytest.fixture
def too_long_user_template_variable_value():
    too_long_value = "a" * (settings.USER_TEMPLATE_VARIABLE_VALUE_MAX_LENGTH + 1)
    return {"name": too_long_value}


@pytest.fixture
def unknown_user_template_variable():
    return {"unsubscribe_link": "some_link"}


@pytest.fixture
def force_test_send_limit():
    limit = settings.TEST_SENDS_PER_DAY
    settings.TEST_SENDS_PER_DAY = 0
    yield
    settings.TEST_SENDS_PER_DAY = limit


@pytest.fixture
def unlimited_org_campaign(campaign_with_letter, org_id):
    save = settings.TEST_SENDS_PER_DAY_FOR_ORG
    settings.TEST_SENDS_PER_DAY_FOR_ORG = {org_id: 2147483647}
    yield campaign_with_letter
    settings.TEST_SENDS_PER_DAY_FOR_ORG = save


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, mock_directory_domains, mock_gendarme, tvm_api_client):
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


def test_missing_recipients(campaign_with_letter, user_id):
    response = _do_test_send_request(user_id, campaign_with_letter)
    assert_validation_error(response, "recipients", "not_found")


def test_empty_recipients(campaign_with_letter, user_id):
    response = _do_test_send_request(user_id, campaign_with_letter, [])
    assert_validation_error(response, "recipients", "empty")


def test_incorrect_recipient(campaign_with_letter, user_id):
    response = _do_test_send_request(user_id, campaign_with_letter, ["email_without_<at>"])
    assert_invalid_emails_error(response, ["email_without_<at>"])


def test_too_many_recipients(campaign_with_letter, many_recipients, user_id):
    response = _do_test_send_request(user_id, campaign_with_letter, many_recipients)
    assert_validation_error(response, "recipients", "too_long")


def test_stores_user_template_variables(
    campaign_with_letter, recipients, user_id, correct_user_template_variables
):
    response = _do_test_send_request(
        user_id, campaign_with_letter, recipients, correct_user_template_variables
    )
    assert_status_code(response, status.HTTP_200_OK)
    tasks = Task.objects.all()
    assert len(tasks) == 1
    assert tasks[0].user_template_variables == {"name": "Someone"}


def test_stores_user_template_variables_in_lower_case(
    campaign_with_letter, recipients, user_id, correct_user_template_variables_with_capital_letters
):
    response = _do_test_send_request(
        user_id,
        campaign_with_letter,
        recipients,
        correct_user_template_variables_with_capital_letters,
    )
    assert_status_code(response, status.HTTP_200_OK)
    tasks = Task.objects.all()
    assert len(tasks) == 1
    assert tasks[0].user_template_variables == {"name": "Someone"}


def test_response_contains_user_template_variables(
    campaign_with_letter, recipients, user_id, correct_user_template_variables
):
    response = _do_test_send_request(
        user_id, campaign_with_letter, recipients, correct_user_template_variables
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert response.json()["user_template_variables"] == correct_user_template_variables


def test_response_contains_user_template_variables_in_lower_case(
    campaign_with_letter, recipients, user_id, correct_user_template_variables_with_capital_letters
):
    response = _do_test_send_request(
        user_id,
        campaign_with_letter,
        recipients,
        correct_user_template_variables_with_capital_letters,
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert response.json()["user_template_variables"] == {"name": "Someone"}


def test_duplicated_user_template_variables_in_different_cases(
    campaign_with_letter, recipients, user_id, duplicated_user_template_variables_in_different_cases
):
    response = _do_test_send_request(
        user_id,
        campaign_with_letter,
        recipients,
        duplicated_user_template_variables_in_different_cases,
    )
    assert_validation_error(
        response, "user_template_variables", "duplicated_user_template_variables"
    )


def test_wrong_type_user_template_variables(
    campaign_with_letter, recipients, user_id, wrong_type_user_template_variables
):
    response = _do_test_send_request(
        user_id, campaign_with_letter, recipients, wrong_type_user_template_variables
    )
    assert_validation_error(response, "user_template_variables", "invalid_type")


def test_wrong_type_user_template_variable_value(
    campaign_with_letter, recipients, user_id, wrong_type_user_template_variable_value
):
    response = _do_test_send_request(
        user_id, campaign_with_letter, recipients, wrong_type_user_template_variable_value
    )
    assert_validation_error(response, "user_template_variables", "invalid_type")


def test_too_long_user_template_variable_value(
    campaign_with_letter, recipients, user_id, too_long_user_template_variable_value
):
    response = _do_test_send_request(
        user_id, campaign_with_letter, recipients, too_long_user_template_variable_value
    )
    assert_validation_error(response, "user_template_variables", "too_long")


def test_unknown_user_template_variable(
    campaign_with_letter, recipients, user_id, unknown_user_template_variable
):
    response = _do_test_send_request(
        user_id, campaign_with_letter, recipients, unknown_user_template_variable
    )
    assert_validation_error(response, "user_template_variables", "unknown_user_template_variables")


def test_fails_with_sending_campaign(sending_campaign, recipients, user_id):
    response = _do_test_send_request(user_id, sending_campaign, recipients)
    assert_wrong_state_error(response, "sending", "draft")


def test_fails_with_sent_campaign(sent_campaign, recipients, user_id):
    response = _do_test_send_request(user_id, sent_campaign, recipients)
    assert_wrong_state_error(response, "sent", "draft")


def test_fails_with_failed_campaign(failed_campaign, recipients, user_id):
    response = _do_test_send_request(user_id, failed_campaign, recipients)
    assert_wrong_state_error(response, "failed", "draft")


def test_fails_without_letter(campaign_with_singleusemaillist, recipients, user_id):
    response = _do_test_send_request(user_id, campaign_with_singleusemaillist, recipients)
    assert_not_ready_error(response, "no_letter")


@pytest.mark.parametrize(
    "empty_detail, expected_error",
    [
        (
            "from_email",
            "empty_from_email",
        ),
        (
            "from_name",
            "empty_from_name",
        ),
        (
            "subject",
            "empty_subject",
        ),
    ],
)
def test_fails_with_empty_detail(
    campaign_with_letter, recipients, user_id, empty_detail, expected_error
):
    set_campaign_details(campaign_with_letter, {empty_detail: ""})
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_not_ready_error(response, expected_error)


def test_domain_not_belongs(mock_directory_domains, campaign_with_letter, recipients, user_id):
    mock_directory_domains.resp_owned = False
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_wrong_domain_error(response, "not_belongs")


def test_from_login_check_on(
    campaign_with_wrong_from_login,
    recipients,
    user_id,
    use_session_auth_api_client,
    permit_setting_check_campaign_from_login,
):
    response = _do_test_send_request(user_id, campaign_with_wrong_from_login, recipients)
    assert_wrong_login_error(response, "not_belongs")


def test_from_login_check_off(
    campaign_with_wrong_from_login,
    recipients,
    user_id,
    use_session_auth_api_client,
    prohibit_setting_check_campaign_from_login,
):
    response = _do_test_send_request(user_id, campaign_with_wrong_from_login, recipients)
    assert_status_code(response, 200)
    assert_response_contains_task(response, _task(campaign_with_wrong_from_login, recipients))


def test_domain_no_mx(mock_gendarme, campaign_with_letter, recipients, user_id):
    mock_gendarme.resp_mx = False
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_wrong_domain_error(response, "no_mx")


def test_passed_recipients(campaign_with_letter, recipients, user_id):
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_status_code(response, 200)
    assert_response_contains_task(response, _task(campaign_with_letter, recipients))


def test_passed_recipients_session_auth(
    campaign_with_letter, recipients, user_id, use_session_auth_api_client
):
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_status_code(response, 200)
    assert_response_contains_task(response, _task(campaign_with_letter, recipients))


def test_passed_recipients_wo_auth_localhost(
    campaign_with_letter, recipients, user_id, use_api_client_wo_auth, disable_auth_on_loopback
):
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_status_code(response, 200)
    assert_response_contains_task(response, _task(campaign_with_letter, recipients))


def test_passed_recipients_wo_auth(
    campaign_with_letter, recipients, user_id, use_api_client_wo_auth
):
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_passed_recipients_with_damaged_tvm_credentials(
    campaign_with_letter, recipients, user_id, use_tvm_api_client_with_damaged_ticket
):
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_passed_recipients_with_non_registered_tvm_source(
    campaign_with_letter, recipients, user_id, tvm_clear_api_v1_allowed_services_list
):
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_forbidden_error(response, "forbidden_service")


def test_forbidden_user(campaign_with_letter, recipients):
    response = _do_test_send_request("forbidden_user_id", campaign_with_letter, recipients)
    assert_forbidden_error(response, "forbidden_user")


def test_limit_reached(campaign_with_letter, recipients, user_id, force_test_send_limit):
    response = _do_test_send_request(user_id, campaign_with_letter, recipients)
    assert_limit_reached_error(response, "test_send_limit_reached")


def test_unlimited_organization_ignores_limit(
    unlimited_org_campaign, recipients, user_id, force_test_send_limit
):
    response = _do_test_send_request(user_id, unlimited_org_campaign, recipients)
    assert_status_code(response, status.HTTP_200_OK)


def test_with_new_untrusty_account(
    ready_campaign_from_new_untrusty_account,
    recipients,
    user_id,
    enable_silently_drop_sents_for_new_untrusty_accounts,
):
    response = _do_test_send_request(user_id, ready_campaign_from_new_untrusty_account, recipients)
    assert_status_code(response, 200)
    assert_response_contains_task(
        response, _task(ready_campaign_from_new_untrusty_account, recipients)
    )
    assert len(Task.objects.all()) == 0


def test_with_old_unknown_account(
    ready_campaign_from_old_unknown_account,
    recipients,
    user_id,
    enable_silently_drop_sents_for_new_untrusty_accounts,
):
    response = _do_test_send_request(user_id, ready_campaign_from_old_unknown_account, recipients)
    assert_status_code(response, 200)
    assert_response_contains_task(
        response, _task(ready_campaign_from_old_unknown_account, recipients)
    )
    assert len(Task.objects.all()) == 1


def _do_test_send_request(user_id, campaign, recipients=None, user_template_variables=None):
    account = campaign.account
    url = "/api/v1/test-send-task?user_id={user_id}&account_slug={account_slug}&campaign_slug={campaign_slug}".format(
        user_id=user_id,
        account_slug=account.name,
        campaign_slug=campaign.slug,
    )
    body = {}
    if recipients is not None:
        body["recipients"] = recipients
    if user_template_variables is not None:
        body["user_template_variables"] = user_template_variables
    global client
    return client.post(url, body, format="json")


def _task_from_response(response):
    return response.json()


def _task(campaign, recipients):
    return {
        "recipients": recipients,
        "account_slug": campaign.account.name,
        "campaign_slug": campaign.slug,
    }


def assert_response_contains_task(response, task):
    task_from_response = _task_from_response(response)
    assert task_from_response["recipients"] == task["recipients"]
    assert task_from_response["account_slug"] == task["account_slug"]
    assert task_from_response["campaign_slug"] == task["campaign_slug"]
