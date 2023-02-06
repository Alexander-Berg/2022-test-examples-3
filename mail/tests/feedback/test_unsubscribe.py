import pytest

from fan.feedback.unsubscribe.action import make_campaign_unsubscribe, make_general_unsubscribe

pytestmark = pytest.mark.django_db


@pytest.fixture
def campain_maillist(campaign, maillist):
    campaign.lists.add(maillist)
    campaign.save()
    return maillist


@pytest.fixture
def campain_unsub(campaign, unsub_list):
    campaign.unsubscribe_lists.add(unsub_list)
    campaign.save()
    return unsub_list


@pytest.mark.usefixtures("clean_cache")
def test_general_unsubscribe(campaign, unsub_list_general):
    test_email = "test@example.com"

    make_general_unsubscribe(campaign.id, test_email)

    assert unsub_list_general.elements.filter(email=test_email).exists()
