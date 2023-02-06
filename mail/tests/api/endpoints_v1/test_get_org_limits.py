import pytest
import urllib.request, urllib.parse, urllib.error
from datetime import date, timedelta
from django.conf import settings
from rest_framework import status
from fan.accounts.organizations.limits import DAYS_IN_MONTH
from fan.campaigns.create import create_campaign
from fan.lists.csv_maillist import parse_csv_data, get_subscribers_number
from fan.lists.singleuse import store_csv_maillist_for_campaign
from fan.models.campaign import Campaign
from fan.models.stat import DeliveryErrorStats
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)
from fan.testutils.campaign import set_campaign_stats, set_delivery_stats
from fan.testutils.maillist import store_n_maillists


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


@pytest.fixture
def sent_more_than_month_ago_campaign(sent_campaign):
    sent_date = date.today() - DAYS_IN_MONTH
    DeliveryErrorStats.objects.filter(campaign=sent_campaign).update(stat_date=sent_date)
    return sent_campaign


@pytest.fixture
def sent_less_than_month_ago_campaign(sent_campaign):
    sent_date = date.today() - DAYS_IN_MONTH + timedelta(days=1)
    DeliveryErrorStats.objects.filter(campaign=sent_campaign).update(stat_date=sent_date)
    return sent_campaign


@pytest.fixture
def emails_sent_count(campaign_stats):
    failed_emails = sum(
        [campaign_stats[key] for key in ("unsubscribed_before", "duplicated", "invalid")]
    )
    return campaign_stats["uploaded"] - failed_emails


@pytest.fixture
def another_sending_campaign(account, project, maillist_csv_content):
    campaign = create_campaign(account=account, project=project)
    store_csv_maillist_for_campaign(campaign, maillist_csv_content, "singleusemaillist.csv")
    campaign.state = Campaign.STATUS_SENDING
    campaign.save()
    return campaign


@pytest.fixture
def maillist_size(maillist_csv_content):
    csv_maillist = parse_csv_data(maillist_csv_content)
    return get_subscribers_number(csv_maillist)


def test_missing_user_id(org_id):
    response = do_request(None, org_id)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_org_id(user_id):
    response = do_request(user_id, None)
    assert_validation_error(response, "org_id", "not_found")


def test_user_is_not_org_admin_or_org_account_user(user_id, org_id):
    response = do_request(user_id, org_id)
    assert_forbidden_error(response, "forbidden_user")


def test_user_has_role_in_org_account(user_id, account):
    response = do_request(user_id, account.org_id)
    assert_status_code(response, status.HTTP_200_OK)


def test_user_is_admin_in_org(user_id, org_id, user_admin_in_directory):
    response = do_request(user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)


def test_org_with_default_settings(user_id, org_id, user_admin_in_directory):
    response = do_request(user_id, org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["send_emails_limit"] == settings.DEFAULT_SEND_EMAILS_LIMIT
    assert response_json["upgradable"] == (not settings.DEFAULT_TRUSTY)
    assert response_json["draft_campaigns_limit"] == settings.DRAFT_CAMPAIGNS_LIMIT
    assert response_json["maillists_limit"] == settings.MAILLISTS_LIMIT


def test_org_with_custom_settings(
    user_id, org_with_custom_settings, large_send_emails_limit, trusty, user_admin_in_directory
):
    response = do_request(user_id, org_with_custom_settings)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["send_emails_limit"] == large_send_emails_limit
    assert response_json["upgradable"] == (not trusty)


def test_org_with_overridden_settings(
    user_id, org_with_overridden_settings, user_admin_in_directory
):
    response = do_request(user_id, org_with_overridden_settings["org_id"])
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert (
        response_json["send_emails_limit"]
        == org_with_overridden_settings["SEND_EMAILS_LIMIT_FOR_ORG"]
    )
    assert response_json["upgradable"] == (not org_with_overridden_settings["TRUSTY_FOR_ORG"])
    assert (
        response_json["draft_campaigns_limit"]
        == org_with_overridden_settings["DRAFT_CAMPAIGNS_LIMIT_FOR_ORG"]
    )
    assert (
        response_json["maillists_limit"] == org_with_overridden_settings["MAILLISTS_LIMIT_FOR_ORG"]
    )


def test_do_not_count_failed_campaign(user_id, org_id, failed_campaign):
    response = do_request(user_id, org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["emails_sent"] == 0
    assert response_json["emails_scheduled"] == 0
    assert response_json["draft_campaigns_count"] == 0


def test_do_not_count_sent_campaign_outside_of_period(
    user_id, org_id, sent_more_than_month_ago_campaign
):
    response = do_request(user_id, org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["emails_sent"] == 0
    assert response_json["emails_scheduled"] == 0
    assert response_json["draft_campaigns_count"] == 0


def test_count_sent_campaign_inside_of_period(
    user_id, org_id, sent_less_than_month_ago_campaign, emails_sent_count
):
    response = do_request(user_id, org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["emails_sent"] == emails_sent_count
    assert response_json["emails_scheduled"] == 0


def test_count_sent_sending_and_draft_campaign(
    user_id,
    org_id,
    sent_campaign,
    emails_sent_count,
    another_sending_campaign,
    maillist_size,
    another_campaign,
):
    response = do_request(user_id, org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["emails_sent"] == emails_sent_count
    assert response_json["emails_scheduled"] == maillist_size
    assert response_json["draft_campaigns_count"] == 1


@pytest.mark.parametrize("draft_campaigns_count", [0, 1, 3])
def test_count_draft_campaigns(user_id, account, project, draft_campaigns_count):
    create_draft_campaigns_for_account(account, project, draft_campaigns_count)
    response = do_request(user_id, account.org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["emails_sent"] == 0
    assert response_json["emails_scheduled"] == 0
    assert response_json["draft_campaigns_count"] == draft_campaigns_count


@pytest.mark.parametrize("sent_campaigns_count", [0, 1, 3])
def test_count_sent_campaigns(
    user_id, account, project, sent_campaigns_count, campaign_stats, emails_sent_count
):
    create_sent_campaigns_for_account(account, project, sent_campaigns_count, campaign_stats)
    response = do_request(user_id, account.org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["emails_sent"] == sent_campaigns_count * emails_sent_count
    assert response_json["emails_scheduled"] == 0
    assert response_json["draft_campaigns_count"] == 0


@pytest.mark.parametrize("sending_campaigns_count", [0, 1, 3])
def test_count_sending_campaigns(
    user_id,
    account,
    project,
    sending_campaigns_count,
    maillist_csv_content,
    maillist_size,
):
    create_sending_campaigns_for_account(
        account, project, sending_campaigns_count, maillist_csv_content
    )
    response = do_request(user_id, account.org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["emails_sent"] == 0
    assert response_json["emails_scheduled"] == maillist_size * sending_campaigns_count
    assert response_json["draft_campaigns_count"] == 0


@pytest.mark.parametrize("maillists_count", [0, 1, 3])
def test_count_maillists(user_id, account, maillists_count):
    store_n_maillists(account, maillists_count)
    response = do_request(user_id, account.org_id)
    response_json = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert response_json["maillists_count"] == maillists_count


def do_request(user_id, org_id):
    args = {
        "user_id": user_id,
        "org_id": org_id,
    }
    url = "/api/v1/org-limits?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.get(url)


def create_draft_campaigns_for_account(account, project, draft_campaings_count):
    for _ in range(draft_campaings_count):
        create_campaign(account=account, project=project)


def create_sent_campaigns_for_account(account, project, sent_campaings_count, campaign_stats):
    for _ in range(sent_campaings_count):
        campaign = create_campaign(account=account, project=project, state=Campaign.STATUS_SENT)
        set_campaign_stats(campaign, campaign_stats)
        set_delivery_stats(campaign, campaign_stats)


def create_sending_campaigns_for_account(
    account, project, sending_campaings_count, maillist_csv_content
):
    for _ in range(sending_campaings_count):
        campaign = create_campaign(account=account, project=project)
        store_csv_maillist_for_campaign(campaign, maillist_csv_content, "maillist.csv")
        campaign.state = Campaign.STATUS_SENDING
        campaign.save()
