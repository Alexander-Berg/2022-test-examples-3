import pytest
from fan.models import Campaign
from fan.campaigns.create import create_campaign
from fan.campaigns.set import clone_campaign_details, ForbiddenCurrentCampaignState


pytestmark = pytest.mark.django_db


@pytest.fixture
def campaign_with_details(account, project):
    campaign = create_campaign(account=account, project=project)
    _set_campaign_details(
        campaign,
        {
            "from_email": "test@from.email",
            "from_name": "test_from_name",
            "subject": "test_subject",
        },
    )
    return campaign


@pytest.fixture
def another_campaign_with_details(account, project):
    campaign = create_campaign(account=account, project=project)
    _set_campaign_details(
        campaign,
        {
            "from_email": "another_test@from.email",
            "from_name": "another_test_from_name",
            "subject": "another_stest_subject",
        },
    )
    return campaign


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_clone_details_fails_on_wrong_campaign_state(campaign, campaign_with_details, wrong_state):
    _set_campaign_state(campaign, wrong_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        clone_campaign_details(campaign, campaign_with_details)


def test_clone_details(campaign, campaign_with_details):
    clone_campaign_details(campaign, campaign_with_details)
    assert campaign.from_email == campaign_with_details.from_email
    assert campaign.from_name == campaign_with_details.from_name
    assert campaign.subject == campaign_with_details.subject


def test_clone_empty_details_clears_details(campaign_with_details, campaign):
    clone_campaign_details(campaign_with_details, campaign)
    assert campaign_with_details.from_email == ""
    assert campaign_with_details.from_name == ""
    assert campaign_with_details.subject == ""


def test_clone_details_rewrites_details(campaign_with_details, another_campaign_with_details):
    clone_campaign_details(campaign_with_details, another_campaign_with_details)
    assert campaign_with_details.from_email == another_campaign_with_details.from_email
    assert campaign_with_details.from_name == another_campaign_with_details.from_name
    assert campaign_with_details.subject == another_campaign_with_details.subject


def _set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()


def _set_campaign_details(campaign, details):
    letter = campaign.default_letter
    letter.subject = details["subject"]
    letter.from_name = details["from_name"]
    letter.from_email = details["from_email"]
    letter.save()
