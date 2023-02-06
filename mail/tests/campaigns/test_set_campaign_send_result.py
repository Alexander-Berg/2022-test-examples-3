import pytest
from django.db import transaction, OperationalError
from fan.campaigns.get import get_locked_campaign_by_slug
from fan.campaigns.exceptions import (
    ForbiddenTargetCampaignState,
    ForbiddenCurrentCampaignState,
    UnsupportedStat,
    InvalidStatValue,
    MismatchedStatsSummary,
)
from fan.campaigns.set import SEND_STAT_KEYS, set_campaign_send_result
from fan.models import Campaign, DeliveryErrorStats


pytestmark = pytest.mark.django_db


KEY_TO_SEND_STAT = {v: k for k, v in list(SEND_STAT_KEYS.items())}


@pytest.mark.parametrize(
    "wrong_dst_state",
    [
        state
        for state in Campaign.VALID_GLOBAL_STATES
        if state not in [Campaign.STATUS_SENT, Campaign.STATUS_FAILED]
    ],
)
def test_wrong_dst_state(sending_campaign, wrong_dst_state, sent_stats):
    with pytest.raises(ForbiddenTargetCampaignState):
        set_campaign_send_result(sending_campaign, wrong_dst_state, sent_stats)


@pytest.mark.parametrize(
    "wrong_src_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_SENDING],
)
def test_wrong_src_state(sending_campaign, wrong_src_state, sent_state, sent_stats):
    _force_set_campaign_state(sending_campaign, wrong_src_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        set_campaign_send_result(sending_campaign, sent_state, sent_stats)


def test_unsupported_stat(sending_campaign, sent_state):
    with pytest.raises(UnsupportedStat, match=r"unsupported_stat"):
        set_campaign_send_result(sending_campaign, sent_state, {"unsupported_stat": 123})


def test_invalid_stat_value(sending_campaign, sent_state):
    with pytest.raises(InvalidStatValue, match=r"email_uploaded"):
        set_campaign_send_result(sending_campaign, sent_state, {"email_uploaded": "123"})


def test_mismatched_stats_summary(sending_campaign, sent_state):
    with pytest.raises(MismatchedStatsSummary):
        set_campaign_send_result(
            sending_campaign, sent_state, {"email_uploaded": 0, "email_unsubscribed": 1}
        )


@pytest.mark.parametrize(
    "dst_state", [state for state in [Campaign.STATUS_SENT, Campaign.STATUS_FAILED]]
)
def test_set_state(sending_campaign, dst_state):
    set_campaign_send_result(sending_campaign, dst_state, {})
    assert (
        Campaign.objects.get(
            slug=sending_campaign.slug, account_id=sending_campaign.account.id
        ).state
        == dst_state
    )


def test_sent_stats_saved(sending_campaign, sent_state, sent_stats):
    set_campaign_send_result(sending_campaign, sent_state, sent_stats)
    assert sent_stats == _saved_stats(sending_campaign)


@pytest.mark.django_db(transaction=True)
@pytest.mark.parametrize(
    "dst_state", [state for state in [Campaign.STATUS_SENT, Campaign.STATUS_FAILED]]
)
def test_set_state_concurrently_fails(sending_campaign, dst_state):
    with pytest.raises(OperationalError):
        with transaction.atomic(using="alternate"):
            already_locked_campaign = get_locked_campaign_by_slug(
                sending_campaign.slug, sending_campaign.account.id, using="alternate"
            )
            set_campaign_send_result(sending_campaign, dst_state, {})


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()


def _saved_stats(campaign):
    stats = DeliveryErrorStats.objects.filter(campaign=campaign, letter=campaign.default_letter)
    return {KEY_TO_SEND_STAT[key.status]: key.count for key in stats}
