import pytest
from fan.models.campaign import Campaign
from fan.campaigns.create import create_campaign


pytestmark = pytest.mark.django_db


def test_create_with_defaults(account):
    campaign = create_campaign(account)
    assert campaign.account == account
    assert campaign.created_by == "_root"
    assert campaign.type == Campaign.TYPE_SIMPLE
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.project == account.get_default_project()
    assert campaign.testing_emails == account.default_testing_emails


def test_create_with_custom_attrs(account, user_id):
    campaign = create_campaign(account, owner=user_id, state=Campaign.STATUS_SENT)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_SENT
