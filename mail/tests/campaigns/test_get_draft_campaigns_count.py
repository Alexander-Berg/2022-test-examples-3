import pytest
from fan.campaigns.get import get_draft_campaigns_count


pytestmark = pytest.mark.django_db


def test_no_campaigns(org_id):
    assert get_draft_campaigns_count(org_id) == 0


def test_org_with_campaign(org_id, campaign):
    assert get_draft_campaigns_count(org_id) == 1


def test_org_with_other_campaigns(org_id, sent_campaign, sending_campaign, failed_campaign):
    assert get_draft_campaigns_count(org_id) == 0
