import pytest
from django.conf import settings
from fan.accounts.organizations.limits import is_campaigns_per_day_send_limit_will_be_exceeded


pytestmark = pytest.mark.django_db


def test_limit_exceeded(sending_campaign):
    _make_multiple_campaigns(sending_campaign, settings.CAMPAIGNS_PER_DAY)
    exceeded, _ = is_campaigns_per_day_send_limit_will_be_exceeded(sending_campaign)
    assert exceeded is True


def test_limit_not_exceeded(sending_campaign):
    _make_multiple_campaigns(sending_campaign, settings.CAMPAIGNS_PER_DAY - 1)
    exceeded, _ = is_campaigns_per_day_send_limit_will_be_exceeded(sending_campaign)
    assert exceeded is False


def _make_multiple_campaigns(origin_campaign, total_count):
    return [_duplicate_campaign(origin_campaign) for _ in range(total_count - 1)]


def _duplicate_campaign(campaign):
    campaign.pk = None
    campaign.slug = None
    campaign.save()
    return campaign
