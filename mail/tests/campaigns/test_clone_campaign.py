import pytest
from fan.models.campaign import Campaign
from fan.campaigns.create import clone_campaign


pytestmark = pytest.mark.django_db


def test_clone_empty_campaign(user_id, account, campaign):
    campaign = clone_campaign(user_id, account, campaign)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.estimated_subscribers_number() == 0
    assert len(campaign.from_email) == 0
    assert len(campaign.from_name) == 0
    assert len(campaign.subject) == 0
    assert len(campaign.default_letter.html_body) == 0


def test_clone_campaign_with_letter(user_id, account, campaign_with_letter):
    campaign = clone_campaign(user_id, account, campaign_with_letter)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.estimated_subscribers_number() == 0
    assert len(campaign.from_email) != 0
    assert len(campaign.from_name) != 0
    assert len(campaign.subject) != 0
    assert len(campaign.default_letter.html_body) != 0


def test_clone_campaign_with_singleusemaillist(user_id, account, campaign_with_singleusemaillist):
    campaign = clone_campaign(user_id, account, campaign_with_singleusemaillist)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.estimated_subscribers_number() != 0
    assert len(campaign.from_email) == 0
    assert len(campaign.from_name) == 0
    assert len(campaign.subject) == 0
    assert len(campaign.default_letter.html_body) == 0


def test_clone_campaign_with_maillist(user_id, account, campaign_with_maillist):
    campaign = clone_campaign(user_id, account, campaign_with_maillist)
    assert campaign.account == account
    assert campaign.maillist == campaign_with_maillist.maillist
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.maillist_size == 2
    assert len(campaign.from_email) == 0
    assert len(campaign.from_name) == 0
    assert len(campaign.subject) == 0
    assert len(campaign.default_letter.html_body) == 0


def test_clone_ready_campaign(user_id, account, ready_campaign):
    campaign = clone_campaign(user_id, account, ready_campaign)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.estimated_subscribers_number() != 0
    assert len(campaign.from_email) != 0
    assert len(campaign.from_name) != 0
    assert len(campaign.subject) != 0
    assert len(campaign.default_letter.html_body) != 0


def test_clone_sending_campaign(user_id, account, sending_campaign):
    campaign = clone_campaign(user_id, account, sending_campaign)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.estimated_subscribers_number() != 0
    assert len(campaign.from_email) != 0
    assert len(campaign.from_name) != 0
    assert len(campaign.subject) != 0
    assert len(campaign.default_letter.html_body) != 0


def test_clone_sent_campaign(user_id, account, sent_campaign):
    campaign = clone_campaign(user_id, account, sent_campaign)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.estimated_subscribers_number() != 0
    assert len(campaign.from_email) != 0
    assert len(campaign.from_name) != 0
    assert len(campaign.subject) != 0
    assert len(campaign.default_letter.html_body) != 0


def test_clone_failed_campaign(user_id, account, failed_campaign):
    campaign = clone_campaign(user_id, account, failed_campaign)
    assert campaign.account == account
    assert campaign.created_by == user_id
    assert campaign.state == Campaign.STATUS_DRAFT
    assert campaign.estimated_subscribers_number() != 0
    assert len(campaign.from_email) != 0
    assert len(campaign.from_name) != 0
    assert len(campaign.subject) != 0
    assert len(campaign.default_letter.html_body) != 0
