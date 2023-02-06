import pytest
from fan.accounts.organizations.limits import get_campaigns_scheduled_count


pytestmark = pytest.mark.django_db


def test_no_campaigns(org_id):
    assert get_campaigns_scheduled_count(org_id) == 0


def test_do_not_count_draft_campaign(org_id, campaign):
    assert get_campaigns_scheduled_count(org_id) == 0


def test_do_not_count_failed_campaign(org_id, failed_campaign):
    assert get_campaigns_scheduled_count(org_id) == 0


def test_do_not_count_sent_campaign(org_id, sent_campaign):
    assert get_campaigns_scheduled_count(org_id) == 0


def test_do_not_count_another_organization_campaign(org_id, sending_campaign):
    assert get_campaigns_scheduled_count(org_id) == 1
    assert get_campaigns_scheduled_count("another_org_id") == 0


def test_count_sending_campaign(org_id, sending_campaign):
    assert get_campaigns_scheduled_count(org_id) == 1
