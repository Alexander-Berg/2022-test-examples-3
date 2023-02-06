import pytest
from fan.lists.unsubscribed import get_unsubscribed_emails_for_campaign

pytestmark = pytest.mark.django_db


def test_with_unsubscribers(campaign_with_unsubscribers):
    unsubscribed = get_unsubscribed_emails_for_campaign(campaign_with_unsubscribers)
    assert len(unsubscribed) == 3


def test_without_unsubscribers(campaign):
    unsubscribed = get_unsubscribed_emails_for_campaign(campaign)
    assert len(unsubscribed) == 0
