import pytest
from flaky import flaky
from fan.campaigns.create import create_campaign
from fan.campaigns.delete import delete_campaign
from fan.campaigns.exceptions import ForbiddenCurrentCampaignState
from fan.message.letter import load_letter
from fan.models import Letter, SingleUseMailList, LetterAttachment
from fan.testutils.letter import load_test_letter


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_avatars_publish, mock_avatars_unpublish):
    pass


@pytest.fixture
def letter_zip():
    letter_file = load_test_letter("letter.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def campaign_with_attachments(account, project, letter_zip):
    campaign = create_campaign(account=account, project=project)
    load_letter(campaign.default_letter, letter_zip)
    assert _account_letter_attachments_count(account) == 2
    return campaign


def test_delete_empty_campaign(account, campaign):
    delete_campaign(campaign)
    assert _account_campaigns_count(account) == 0
    assert _account_letters_count(account) == 0


def test_delete_campaign_with_letter(account, campaign_with_letter):
    delete_campaign(campaign_with_letter)
    assert _account_campaigns_count(account) == 0
    assert _account_letters_count(account) == 0


def test_delete_campaign_with_singleusemaillist(account, campaign_with_singleusemaillist):
    delete_campaign(campaign_with_singleusemaillist)
    assert _account_campaigns_count(account) == 0
    assert _account_letters_count(account) == 0
    assert _account_maillists_count(account) == 0


def test_delete_ready_campaign(account, ready_campaign):
    delete_campaign(ready_campaign)
    assert _account_campaigns_count(account) == 0
    assert _account_letters_count(account) == 0
    assert _account_maillists_count(account) == 0


@flaky(max_runs=5)
def test_delete_campaign_with_attachments(
    account, campaign_with_attachments, mock_avatars_unpublish
):
    delete_campaign(campaign_with_attachments)
    assert _account_campaigns_count(account) == 0
    assert _account_letters_count(account) == 0
    assert _account_letter_attachments_count(account) == 0
    assert len(mock_avatars_unpublish.unpublished_images) == 2


def test_delete_sending_campaign_raises_exception(sending_campaign):
    with pytest.raises(ForbiddenCurrentCampaignState):
        delete_campaign(sending_campaign)


def test_delete_sent_campaign_raises_exception(sent_campaign):
    with pytest.raises(ForbiddenCurrentCampaignState):
        delete_campaign(sent_campaign)


def test_delete_failed_campaign_raises_exception(failed_campaign):
    with pytest.raises(ForbiddenCurrentCampaignState):
        delete_campaign(failed_campaign)


def _account_campaigns_count(account):
    return account.campaign_set.all().count()


def _account_letters_count(account):
    return Letter.objects.filter(campaign__account__exact=account).count()


def _account_maillists_count(account):
    return SingleUseMailList.objects.filter(campaign__account__exact=account).count()


def _account_letter_attachments_count(account):
    return LetterAttachment.objects.filter(letter__campaign__account__exact=account).count()
