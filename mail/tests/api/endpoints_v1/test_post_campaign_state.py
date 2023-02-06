import pytest
from rest_framework import status
from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_wrong_state_error,
    assert_not_ready_error,
    assert_wrong_domain_error,
    assert_forbidden_error,
    assert_limit_reached_error,
    assert_wrong_login_error,
)
from fan.models import Campaign
from fan.campaigns.set import set_campaign_details
from django.conf import settings


pytestmark = pytest.mark.django_db


def test_unexisted_account(tvm_api_client, user_id, ready_campaign, sending_state):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, "unexisted_account_slug", ready_campaign.slug, sending_state
    )
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_unexisted_campaign(tvm_api_client, user_id, account, sending_state):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, "unexisted_campaign_slug", sending_state
    )
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_empty_user_id(tvm_api_client, account, ready_campaign, sending_state):
    response = _do_post_campaign_state_request(
        tvm_api_client, "", account.name, ready_campaign.slug, sending_state
    )
    assert_validation_error(response, "user_id", "empty")


def test_empty_account_slug(tvm_api_client, user_id, ready_campaign, sending_state):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, "", ready_campaign.slug, sending_state
    )
    assert_validation_error(response, "account_slug", "empty")


def test_empty_campaign_slug(tvm_api_client, user_id, account, sending_state):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, "", sending_state
    )
    assert_validation_error(response, "campaign_slug", "empty")


def test_empty_state(tvm_api_client, user_id, account, ready_campaign):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, ""
    )
    assert_validation_error(response, "state", "empty")


def test_unexisted_state(tvm_api_client, user_id, account, ready_campaign):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, "unexisted_state"
    )
    assert_validation_error(response, "state", "not_supported")


@pytest.mark.parametrize(
    "wrong_dst_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_SENDING],
)
def test_wrong_dst_state(tvm_api_client, user_id, account, ready_campaign, wrong_dst_state):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, wrong_dst_state
    )
    assert_validation_error(response, "state", "not_supported")


@pytest.mark.parametrize(
    "wrong_src_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_wrong_src_state(
    tvm_api_client, user_id, account, ready_campaign, wrong_src_state, sending_state
):
    _force_set_campaign_state(ready_campaign, wrong_src_state)
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_wrong_state_error(response, wrong_src_state, "draft")


def test_no_maillist(
    tvm_api_client,
    user_id,
    account,
    campaign_with_letter,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, campaign_with_letter.slug, sending_state
    )
    assert_not_ready_error(response, "no_maillist")


def test_no_letter(
    tvm_api_client, user_id, account, campaign_with_singleusemaillist, sending_state
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, campaign_with_singleusemaillist.slug, sending_state
    )
    assert_not_ready_error(response, "no_letter")


def test_domain_not_belongs(
    tvm_api_client,
    user_id,
    account,
    ready_campaign,
    from_login,
    sending_state,
    mock_tvm,
    mock_directory_domains,
):
    set_campaign_details(ready_campaign, {"from_email": "{}@foreign_domain.ru".format(from_login)})
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_wrong_domain_error(response, "not_belongs")


def test_from_login_check_on(
    tvm_api_client,
    user_id,
    account,
    campaign_with_wrong_from_login,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    permit_setting_check_campaign_from_login,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, campaign_with_wrong_from_login.slug, sending_state
    )
    assert_wrong_login_error(response, "not_belongs")


def test_from_login_check_off(
    tvm_api_client,
    user_id,
    account,
    campaign_with_wrong_from_login,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    prohibit_setting_check_campaign_from_login,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, campaign_with_wrong_from_login.slug, sending_state
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert (
        Campaign.objects.get(
            slug=campaign_with_wrong_from_login.slug,
            account_id=campaign_with_wrong_from_login.account.id,
        ).state
        == sending_state
    )


def test_domain_no_mx(
    tvm_api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    mock_gendarme.resp_mx = False
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_wrong_domain_error(response, "no_mx")


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
def test_empty_detail(
    tvm_api_client, user_id, account, ready_campaign, sending_state, empty_detail, expected_error
):
    _clear_campaign_detail(ready_campaign, empty_detail)
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_not_ready_error(response, expected_error)


def test_set_sending_state(
    tvm_api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    do_test_set_sending_state(
        tvm_api_client,
        user_id,
        account,
        ready_campaign,
        sending_state,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
    )


def test_set_sending_state_session_auth(
    auth_api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    do_test_set_sending_state(
        auth_api_client,
        user_id,
        account,
        ready_campaign,
        sending_state,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
    )


def test_set_sending_state_wo_auth_localhost(
    api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    disable_auth_on_loopback,
):
    do_test_set_sending_state(
        api_client,
        user_id,
        account,
        ready_campaign,
        sending_state,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
    )


def do_test_set_sending_state(
    client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    response = _do_post_campaign_state_request(
        client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert (
        Campaign.objects.get(slug=ready_campaign.slug, account_id=ready_campaign.account.id).state
        == sending_state
    )


def test_set_sending_state_wo_auth(
    api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    response = _do_post_campaign_state_request(
        api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_set_sending_state_with_damaged_tvm_credentials(
    tvm_api_client_with_damaged_ticket,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    response = _do_post_campaign_state_request(
        tvm_api_client_with_damaged_ticket,
        user_id,
        account.name,
        ready_campaign.slug,
        sending_state,
    )
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_set_sending_state_with_non_registered_tvm_source(
    tvm_api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    tvm_clear_api_v1_allowed_services_list,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_forbidden_error(response, "forbidden_service")


def test_forbidden_user(
    tvm_api_client,
    account,
    ready_campaign,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, "forbidden_user_id", account.name, ready_campaign.slug, sending_state
    )
    assert_forbidden_error(response, "forbidden_user")


def test_campaigns_per_day_send_limit_reached(
    tvm_api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    force_campaigns_per_day_send_limit,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_limit_reached_error(response, "send_limit_reached")


def test_emails_per_month_send_limit_reached(
    tvm_api_client,
    user_id,
    account,
    ready_campaign,
    sending_state,
    force_emails_per_month_send_limit,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, ready_campaign.slug, sending_state
    )
    assert_limit_reached_error(response, "send_limit_reached")


def test_unlimited_organization_ignores_campaigns_per_day_send_limit(
    tvm_api_client,
    user_id,
    account,
    unlimited_org_campaign,
    sending_state,
    force_campaigns_per_day_send_limit,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, unlimited_org_campaign.slug, sending_state
    )
    assert_status_code(response, status.HTTP_200_OK)


def test_unlimited_organization_ignores_emails_per_month_send_limit(
    tvm_api_client,
    user_id,
    account,
    unlimited_org_campaign,
    sending_state,
    force_emails_per_month_send_limit,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
):
    response = _do_post_campaign_state_request(
        tvm_api_client, user_id, account.name, unlimited_org_campaign.slug, sending_state
    )
    assert_status_code(response, status.HTTP_200_OK)


def test_set_sending_state_for_new_untrusty_account(
    tvm_api_client,
    user_id,
    ready_campaign_from_new_untrusty_account,
    sending_state,
    sent_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    enable_silently_drop_sents_for_new_untrusty_accounts,
):
    response = _do_post_campaign_state_request(
        tvm_api_client,
        user_id,
        ready_campaign_from_new_untrusty_account.account.name,
        ready_campaign_from_new_untrusty_account.slug,
        sending_state,
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert (
        Campaign.objects.get(
            slug=ready_campaign_from_new_untrusty_account.slug,
            account_id=ready_campaign_from_new_untrusty_account.account.id,
        ).state
        == sent_state
    )


def test_set_sending_state_for_old_unknown_account(
    tvm_api_client,
    user_id,
    ready_campaign_from_old_unknown_account,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    enable_silently_drop_sents_for_new_untrusty_accounts,
):
    response = _do_post_campaign_state_request(
        tvm_api_client,
        user_id,
        ready_campaign_from_old_unknown_account.account.name,
        ready_campaign_from_old_unknown_account.slug,
        sending_state,
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert (
        Campaign.objects.get(
            slug=ready_campaign_from_old_unknown_account.slug,
            account_id=ready_campaign_from_old_unknown_account.account.id,
        ).state
        == sending_state
    )


@pytest.fixture
def force_campaigns_per_day_send_limit():
    limit = settings.CAMPAIGNS_PER_DAY
    settings.CAMPAIGNS_PER_DAY = 0
    yield
    settings.CAMPAIGNS_PER_DAY = limit


@pytest.fixture
def force_emails_per_month_send_limit(ready_campaign):
    limit = settings.DEFAULT_SEND_EMAILS_LIMIT
    settings.DEFAULT_SEND_EMAILS_LIMIT = ready_campaign.estimated_subscribers_number() - 1
    yield
    settings.DEFAULT_SEND_EMAILS_LIMIT = limit


@pytest.fixture
def unlimited_org_campaign(ready_campaign, org_id):
    save = settings.CAMPAIGNS_PER_DAY_FOR_ORG, settings.SEND_EMAILS_LIMIT_FOR_ORG
    settings.CAMPAIGNS_PER_DAY_FOR_ORG = {org_id: 2147483647}
    settings.SEND_EMAILS_LIMIT_FOR_ORG = {org_id: 2147483647}
    yield ready_campaign
    settings.CAMPAIGNS_PER_DAY_FOR_ORG, settings.SEND_EMAILS_LIMIT_FOR_ORG = save


@pytest.fixture
def sending_state():
    return Campaign.STATUS_SENDING


def _do_post_campaign_state_request(client, user_id, account_slug, campaign_slug, state):
    url = "/api/v1/campaign-state?user_id={user_id}&account_slug={account_slug}&campaign_slug={campaign_slug}&state={state}".format(
        user_id=user_id,
        account_slug=account_slug,
        campaign_slug=campaign_slug,
        state=state,
    )
    return client.post(url)


def _clear_campaign_detail(campaign, detail_name):
    set_campaign_details(campaign, {detail_name: ""})


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
