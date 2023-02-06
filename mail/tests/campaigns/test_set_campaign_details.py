import pytest
from fan.models import Campaign
from fan.campaigns.exceptions import ForbiddenCurrentCampaignState
from fan.campaigns.set import set_campaign_details

pytestmark = pytest.mark.django_db


def test_set_all_details(campaign, campaign_details):
    set_campaign_details(campaign, campaign_details)
    assert campaign.from_name == campaign_details["from_name"]
    assert campaign.from_email == campaign_details["from_email"]
    assert campaign.subject == campaign_details["subject"]
    assert campaign.title == campaign_details["subject"]


def test_set_some_details(campaign, campaign_details):
    del campaign_details["from_email"]
    set_campaign_details(campaign, campaign_details)
    assert campaign.from_name == campaign_details["from_name"]
    assert campaign.subject == campaign_details["subject"]
    assert campaign.title == campaign_details["subject"]


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_set_details_fails_on_wrong_campaign_state(campaign, campaign_details, wrong_state):
    _force_set_campaign_state(campaign, wrong_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        set_campaign_details(campaign, campaign_details)


@pytest.fixture
def campaign_details():
    return {
        "from_email": "test@from.email",
        "from_name": "test_from_name",
        "subject": "test_subject",
    }


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
