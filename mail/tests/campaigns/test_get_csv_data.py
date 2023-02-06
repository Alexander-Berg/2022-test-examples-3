import pytest
from fan.campaigns.get import get_csv_data


pytestmark = pytest.mark.django_db


def test_on_camapign_with_singleusemaillist(campaign_with_singleusemaillist):
    csv = get_csv_data(campaign_with_singleusemaillist)
    assert csv != None


def test_on_camapign_with_maillist(campaign_with_maillist):
    csv = get_csv_data(campaign_with_maillist)
    assert csv != None


def test_on_camapign_without_maillists(campaign):
    csv = get_csv_data(campaign)
    assert csv == None
