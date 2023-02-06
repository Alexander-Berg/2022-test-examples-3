import pytest
from fan.campaigns.exceptions import ForbiddenCurrentCampaignState
from fan.lists.singleuse import clone_csv_for_campaign
from fan.models import Campaign


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_raises_exception_on_campaign_wrong_state(
    campaign, campaign_with_singleusemaillist, wrong_state
):
    _set_state(campaign, wrong_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        clone_csv_for_campaign(campaign, campaign_with_singleusemaillist)


def test_clone_campaign_without_maillist_into_campaign_with_singleusemaillist(
    another_campaign, campaign_with_singleusemaillist
):
    clone_csv_for_campaign(campaign_with_singleusemaillist, another_campaign)
    campaign_with_singleusemaillist = _refresh_from_db(campaign_with_singleusemaillist)
    assert campaign_with_singleusemaillist.maillist_uploaded == False
    assert campaign_with_singleusemaillist.estimated_subscribers_number() == 0


def test_clone_campaign_without_maillist_into_another_campaign_without_maillist(
    campaign, another_campaign
):
    clone_csv_for_campaign(campaign, another_campaign)
    campaign = _refresh_from_db(campaign)
    assert campaign.maillist_uploaded == False
    assert campaign.estimated_subscribers_number() == 0


def test_clone_campaign_with_singleusemaillist_into_campaign_without_maillist(
    campaign, campaign_with_singleusemaillist
):
    clone_csv_for_campaign(campaign, campaign_with_singleusemaillist)
    campaign = _refresh_from_db(campaign)
    assert campaign.maillist_uploaded == True
    assert (
        campaign.estimated_subscribers_number()
        == campaign_with_singleusemaillist.estimated_subscribers_number()
    )


def test_clones_data_preview(campaign, campaign_with_singleusemaillist):
    clone_csv_for_campaign(campaign, campaign_with_singleusemaillist)
    campaign = _refresh_from_db(campaign)
    assert campaign.maillist_uploaded == True
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]


def test_clone_campaign_with_singleusemaillist_into_another_campaign_with_singleusemaillist(
    another_campaign_with_singleusemaillist, campaign_with_singleusemaillist
):
    clone_csv_for_campaign(campaign_with_singleusemaillist, another_campaign_with_singleusemaillist)
    campaign_with_singleusemaillist = _refresh_from_db(campaign_with_singleusemaillist)
    assert campaign_with_singleusemaillist.maillist_uploaded == True
    assert (
        campaign_with_singleusemaillist.estimated_subscribers_number()
        == another_campaign_with_singleusemaillist.estimated_subscribers_number()
    )


def _set_state(campaign, state):
    campaign.state = state
    campaign.save()


def _refresh_from_db(campaign):
    return Campaign.objects.get(id=campaign.id)
