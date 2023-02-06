import pytest
from fan.lists.maillist import delete_maillist
from fan.models import Campaign


pytestmark = pytest.mark.django_db


def test_on_account_with_maillist(account, maillist):
    delete_maillist(maillist)
    assert len(account.maillists.all()) == 0


def test_on_account_with_maillists(account_with_almost_max_maillists_count):
    delete_maillist(account_with_almost_max_maillists_count.maillists.all()[0])
    assert len(account_with_almost_max_maillists_count.maillists.all()) == 3


def test_resets_maillist_in_campaign(campaign_with_maillist):
    delete_maillist(campaign_with_maillist.maillist)
    campaign = Campaign.objects.get(id=campaign_with_maillist.id)
    assert campaign.maillist == None
