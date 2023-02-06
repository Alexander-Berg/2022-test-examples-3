import pytest
from django.conf import settings
from fan.accounts.organizations.limits import is_draft_campaigns_limit_reached
from fan.models import Campaign


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def mock_draft_campaigns_limit():
    saved = settings.DRAFT_CAMPAIGNS_LIMIT
    settings.DRAFT_CAMPAIGNS_LIMIT = 10
    yield
    settings.DRAFT_CAMPAIGNS_LIMIT = saved


def test_limit_reached(org_id, campaign):
    _make_multiple_campaigns(campaign, settings.DRAFT_CAMPAIGNS_LIMIT)
    assert is_draft_campaigns_limit_reached(org_id) == True


def test_limit_not_exceeded(org_id, campaign):
    _make_multiple_campaigns(campaign, settings.DRAFT_CAMPAIGNS_LIMIT - 1)
    assert is_draft_campaigns_limit_reached(org_id) == False


@pytest.mark.parametrize(
    "state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_count_only_draft_campaigns(org_id, campaign, state):
    _force_set_campaign_state(campaign, state)
    _make_multiple_campaigns(campaign, settings.DRAFT_CAMPAIGNS_LIMIT)
    assert is_draft_campaigns_limit_reached(org_id) == False


def _make_multiple_campaigns(origin_campaign, total_count):
    return [_duplicate_campaign(origin_campaign) for _ in range(total_count - 1)]


def _duplicate_campaign(campaign):
    campaign.pk = None
    campaign.slug = None
    campaign.save()
    return campaign


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
