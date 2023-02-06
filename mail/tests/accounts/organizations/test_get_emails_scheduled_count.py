import pytest
from fan.accounts.organizations.limits import get_emails_scheduled_count
from fan.campaigns.create import create_campaign
from fan.lists.singleuse import store_csv_maillist_for_campaign
from fan.models.campaign import Campaign


pytestmark = pytest.mark.django_db


@pytest.fixture
def another_sending_campaign(account, project, maillist_csv_content):
    campaign = create_campaign(account=account, project=project)
    store_csv_maillist_for_campaign(campaign, maillist_csv_content, "singleusemaillist.csv")
    campaign.state = Campaign.STATUS_SENDING
    campaign.save()
    return campaign


@pytest.fixture
def maillist_size(sending_campaign):
    return sending_campaign.single_use_maillist.subscribers_number


def test_no_campaigns(org_id):
    assert get_emails_scheduled_count(org_id) == 0


def test_do_not_count_draft_campaign(org_id, campaign):
    assert get_emails_scheduled_count(org_id) == 0


def test_do_not_count_failed_campaign(org_id, failed_campaign):
    assert get_emails_scheduled_count(org_id) == 0


def test_do_not_count_sent_campaign(org_id, sent_campaign):
    assert get_emails_scheduled_count(org_id) == 0


def test_do_not_count_another_organization_campaign(org_id, sending_campaign, maillist_size):
    assert get_emails_scheduled_count(org_id) == maillist_size
    assert get_emails_scheduled_count("another_org_id") == 0


def test_count_sending_campaign(org_id, sending_campaign, maillist_size):
    assert get_emails_scheduled_count(org_id) == maillist_size


def test_count_several_sending_campaigns(
    org_id, sending_campaign, another_sending_campaign, maillist_size
):
    assert get_emails_scheduled_count(org_id) == 2 * maillist_size
