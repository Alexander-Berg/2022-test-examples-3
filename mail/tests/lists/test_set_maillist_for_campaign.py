import pytest
from fan.campaigns.exceptions import ForbiddenCurrentCampaignState
from fan.lists.maillist import set_maillist_for_campaign
from fan.models import Campaign, SingleUseMailList


pytestmark = pytest.mark.django_db


def test_sets_maillist(maillist, campaign):
    campaign = set_maillist_for_campaign(campaign, maillist)
    assert campaign.maillist == maillist
    assert campaign.maillist_uploaded == True
    assert campaign.maillist_description == "maillist.csv"
    assert campaign.maillist_preview == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]


def test_overwrites_maillist(another_maillist, campaign_with_maillist):
    campaign_with_maillist = set_maillist_for_campaign(campaign_with_maillist, another_maillist)
    assert campaign_with_maillist.maillist == another_maillist
    assert campaign_with_maillist.maillist_uploaded == True
    assert campaign_with_maillist.maillist_description == "another_maillist.csv"


def test_overwrites_singleusemaillist(maillist, campaign_with_singleusemaillist):
    campaign_with_maillist = set_maillist_for_campaign(campaign_with_singleusemaillist, maillist)
    assert campaign_with_maillist.maillist == maillist
    assert campaign_with_singleusemaillist.maillist_uploaded == True
    assert campaign_with_singleusemaillist.maillist_description == "maillist.csv"


def test_deletes_singleusemaillist(maillist, campaign_with_singleusemaillist):
    campaign_with_singleusemaillist = set_maillist_for_campaign(
        campaign_with_singleusemaillist, maillist
    )
    with pytest.raises(SingleUseMailList.DoesNotExist):
        campaign_with_singleusemaillist.single_use_maillist
    assert len(SingleUseMailList.objects.all()) == 0


def test_does_nothing_for_none_campaign(maillist):
    set_maillist_for_campaign(None, maillist)
    assert len(maillist.campaigns.all()) == 0


def test_raises_on_campaign_from_different_account(maillist, campaign_from_another_account):
    with pytest.raises(RuntimeError, match="maillist does not belong to campaign's account"):
        set_maillist_for_campaign(campaign_from_another_account, maillist)


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_fail_on_setting_maillist_for_wrong_state_campaign(maillist, campaign, wrong_state):
    force_set_campaign_state(campaign, wrong_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        set_maillist_for_campaign(campaign, maillist)


def force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
