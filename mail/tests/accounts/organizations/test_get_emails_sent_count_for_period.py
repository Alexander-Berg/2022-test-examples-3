import pytest
from datetime import date, timedelta
from fan.accounts.organizations.limits import get_emails_sent_count_for_month, DAYS_IN_MONTH
from fan.campaigns.create import create_campaign
from fan.models.campaign import Campaign
from fan.models.stat import DeliveryErrorStats
from fan.testutils.campaign import set_campaign_stats, set_delivery_stats


pytestmark = pytest.mark.django_db


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
def another_sent_campaign(account, project, campaign_stats):
    campaign = create_campaign(account=account, project=project)
    campaign.state = Campaign.STATUS_SENT
    campaign.save()
    set_campaign_stats(campaign, campaign_stats)
    set_delivery_stats(campaign, campaign_stats)
    return campaign


@pytest.fixture
def emails_sent_count(campaign_stats):
    failed_emails = sum(
        [campaign_stats[key] for key in ("unsubscribed_before", "duplicated", "invalid")]
    )
    return campaign_stats["uploaded"] - failed_emails


def test_no_campaigns(org_id):
    assert get_emails_sent_count_for_month(org_id) == 0


def test_do_not_count_draft_campaign(org_id, campaign):
    assert get_emails_sent_count_for_month(org_id) == 0


def test_do_not_count_sending_campaign(org_id, sending_campaign):
    assert get_emails_sent_count_for_month(org_id) == 0


def test_do_not_count_failed_campaign(org_id, failed_campaign):
    assert get_emails_sent_count_for_month(org_id) == 0


def test_do_not_count_sent_campaign_outside_of_period(org_id, sent_more_than_month_ago_campaign):
    assert get_emails_sent_count_for_month(org_id) == 0


def test_count_sent_campaign_inside_of_period(
    org_id, sent_less_than_month_ago_campaign, emails_sent_count
):
    assert get_emails_sent_count_for_month(org_id) == emails_sent_count


def test_count_sent_campaign(org_id, sent_campaign, emails_sent_count):
    assert get_emails_sent_count_for_month(org_id) == emails_sent_count


def test_count_several_sent_campaigns(
    org_id, sent_campaign, another_sent_campaign, emails_sent_count
):
    assert get_emails_sent_count_for_month(org_id) == 2 * emails_sent_count


def test_do_not_count_another_organization_campaign(org_id, sent_campaign, emails_sent_count):
    assert get_emails_sent_count_for_month(org_id) == emails_sent_count
    assert get_emails_sent_count_for_month("another_org_id") == 0
