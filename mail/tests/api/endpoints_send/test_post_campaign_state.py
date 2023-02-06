import pytest
import urllib.request, urllib.parse, urllib.error
from rest_framework import status
from fan.campaigns.set import SEND_STAT_KEYS
from fan.models import Campaign, DeliveryErrorStats
from fan.testutils.matchers import (
    assert_status_code,
    assert_validation_error,
    assert_wrong_state_error,
)


pytestmark = pytest.mark.django_db


KEY_TO_SEND_STAT = {v: k for k, v in list(SEND_STAT_KEYS.items())}


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


def test_unexisted_account(sending_campaign, sent_state, sent_stats):
    response = do_request("unexisted_account_slug", sending_campaign.slug, sent_state, sent_stats)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_unexisted_campaign(account, sent_state, sent_stats):
    response = do_request(account.name, "unexisted_campaign_slug", sent_state, sent_stats)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_empty_account_slug(sending_campaign, sent_state, sent_stats):
    response = do_request("", sending_campaign.slug, sent_state, sent_stats)
    assert_validation_error(response, "account_slug", "empty")


def test_empty_campaign_slug(account, sent_state, sent_stats):
    response = do_request(account.name, "", sent_state, sent_stats)
    assert_validation_error(response, "campaign_slug", "empty")


def test_empty_state(account, sending_campaign, sent_stats):
    response = do_request(account.name, sending_campaign.slug, "", sent_stats)
    assert_validation_error(response, "state", "empty")


def test_unexisted_state(account, sending_campaign, sent_stats):
    response = do_request(account.name, sending_campaign.slug, "unexisted_state", sent_stats)
    assert_validation_error(response, "state", "not_supported")


@pytest.mark.parametrize(
    "wrong_dst_state",
    [
        state
        for state in Campaign.VALID_GLOBAL_STATES
        if state not in [Campaign.STATUS_SENT, Campaign.STATUS_FAILED]
    ],
)
def test_wrong_dst_state(account, sending_campaign, wrong_dst_state, sent_stats):
    response = do_request(account.name, sending_campaign.slug, wrong_dst_state, sent_stats)
    assert_validation_error(response, "state", "not_supported")


@pytest.mark.parametrize(
    "wrong_src_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_SENDING],
)
def test_wrong_src_state(account, sending_campaign, wrong_src_state, sent_state, sent_stats):
    _force_set_campaign_state(sending_campaign, wrong_src_state)
    response = do_request(account.name, sending_campaign.slug, sent_state, sent_stats)
    assert_wrong_state_error(response, wrong_src_state, "sending")


def test_unsupported_stat(account, sending_campaign, sent_state):
    response = do_request(
        account.name,
        sending_campaign.slug,
        sent_state,
        {"unsupported_stat": 123},
    )
    assert_validation_error(response, "unsupported_stat", "not_supported")


def test_invalid_stat_value(account, sending_campaign, sent_state):
    response = do_request(
        account.name,
        sending_campaign.slug,
        sent_state,
        {"email_uploaded": "123"},
    )
    assert_validation_error(response, "email_uploaded", "invalid_type")


def test_mismatched_stats_summary(account, sending_campaign, sent_state):
    response = do_request(
        account.name,
        sending_campaign.slug,
        sent_state,
        {"email_uploaded": 0, "email_unsubscribed": 1},
    )
    assert_validation_error(response, "non_field", "mismatched_summary")


def test_set_sent_state(account, sending_campaign, sent_state, sent_stats):
    response = do_request(account.name, sending_campaign.slug, sent_state, sent_stats)
    assert_status_code(response, status.HTTP_200_OK)
    assert (
        Campaign.objects.get(slug=sending_campaign.slug, account_id=account.id).state == sent_state
    )
    assert sent_stats == _saved_stats(sending_campaign)


def test_set_failed_state(account, sending_campaign):
    response = do_request(
        account.name,
        sending_campaign.slug,
        Campaign.STATUS_FAILED,
        {"error": "test"},
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert (
        Campaign.objects.get(slug=sending_campaign.slug, account_id=account.id).state
        == Campaign.STATUS_FAILED
    )


def do_request(account_slug, campaign_slug, state, stats):
    args = {
        "account_slug": account_slug,
        "campaign_slug": campaign_slug,
        "state": state,
    }
    url = "/api/send/campaign-state?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.post(url, stats, format="json")


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()


def _saved_stats(campaign):
    stats = DeliveryErrorStats.objects.filter(campaign=campaign, letter=campaign.default_letter)
    return {KEY_TO_SEND_STAT[key.status]: key.count for key in stats}
