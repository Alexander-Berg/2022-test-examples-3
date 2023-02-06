import pytest

from fan.models import Campaign

pytestmark = pytest.mark.django_db


def test_new_campaign_has_default_unsubscribe_list(account):
    c = Campaign(project=account.project_set.first(), account=account, type=Campaign.TYPE_SIMPLE)
    c.save()

    assert c.unsubscribe_lists.exists()


@pytest.mark.parametrize("_name", [None, "", "test_name"])
def test_campaign_title(_name, campaign, letter):
    campaign.name = _name

    _expected = _name or letter.subject

    assert campaign.title == _expected
