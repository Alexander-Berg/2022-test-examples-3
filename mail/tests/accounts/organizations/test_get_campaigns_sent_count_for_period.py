import pytest
from datetime import date, timedelta
from fan.accounts.organizations.limits import get_campaigns_sent_count_for_period
from fan.models import DeliveryErrorStats


pytestmark = pytest.mark.django_db


def test_no_campaigns(org_id):
    assert get_campaigns_sent_count_for_period(org_id, 1) == 0


def test_do_not_count_draft_campaign(org_id, campaign):
    assert get_campaigns_sent_count_for_period(org_id, 1) == 0


def test_do_not_count_sending_campaign(org_id, sending_campaign):
    assert get_campaigns_sent_count_for_period(org_id, 1) == 0


def test_do_not_count_failed_campaign(org_id, failed_campaign):
    assert get_campaigns_sent_count_for_period(org_id, 1) == 0


def test_do_not_count_sent_campaign_outside_of_period(org_id, sent_yesterday_campaign):
    assert get_campaigns_sent_count_for_period(org_id, 1) == 0


def test_count_sent_campaign_inside_of_period(org_id, sent_yesterday_campaign):
    assert get_campaigns_sent_count_for_period(org_id, 2) == 1


def test_do_not_count_another_organization_campaign(org_id, sent_campaign):
    assert get_campaigns_sent_count_for_period(org_id, 1) == 1
    assert get_campaigns_sent_count_for_period("another_org_id", 1) == 0


@pytest.fixture
def sent_yesterday_campaign(sent_campaign):
    DeliveryErrorStats.objects.filter(campaign=sent_campaign).update(
        stat_date=date.today() - timedelta(days=1)
    )
    return sent_campaign
