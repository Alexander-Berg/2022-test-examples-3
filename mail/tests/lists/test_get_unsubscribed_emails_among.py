import pytest
from fan.lists.unsubscribed import get_unsubscribed_emails_among


pytestmark = pytest.mark.django_db


@pytest.fixture
def empty_unsubscribed_emails():
    return []


@pytest.fixture
def unsubscribers_subset(unsubscribers):
    return unsubscribers[1:]


@pytest.fixture
def unsubscribers_superset(unsubscribers):
    return unsubscribers + ["unsubscriber4@domain.ru"]


def test_among_empty_unsubscribed_emails_on_account_with_unsubscribers(
    campaign_with_unsubscribers, empty_unsubscribed_emails
):
    result = get_unsubscribed_emails_among(
        campaign_with_unsubscribers.account, empty_unsubscribed_emails
    )
    assert len(result) == 0
    assert result == set(empty_unsubscribed_emails)


def test_among_empty_unsubscribed_emails_on_account_without_unsubscribers(
    campaign, empty_unsubscribed_emails
):
    result = get_unsubscribed_emails_among(campaign.account, empty_unsubscribed_emails)
    assert len(result) == 0
    assert result == set(empty_unsubscribed_emails)


def test_among_all_unsubscribed_emails_on_account_with_unsubscribers(
    campaign_with_unsubscribers, unsubscribers
):
    result = get_unsubscribed_emails_among(campaign_with_unsubscribers.account, unsubscribers)
    assert len(result) == 3
    assert result == set(unsubscribers)


def test_among_all_unsubscribed_emails_on_account_without_unsubscribers(campaign, unsubscribers):
    result = get_unsubscribed_emails_among(campaign.account, unsubscribers)
    assert len(result) == 0
    assert result == set([])


def test_among_unsubscribed_emails_subset_on_account_with_unsubscribers(
    campaign_with_unsubscribers, unsubscribers_subset
):
    result = get_unsubscribed_emails_among(
        campaign_with_unsubscribers.account, unsubscribers_subset
    )
    assert len(result) == 2
    assert result == set(unsubscribers_subset)


def test_among_unsubscribed_emails_subset_on_account_without_unsubscribers(
    campaign, unsubscribers_subset
):
    result = get_unsubscribed_emails_among(campaign.account, unsubscribers_subset)
    assert len(result) == 0
    assert result == set([])


def test_among_unsubscribed_emails_superset_on_account_with_unsubscribers(
    campaign_with_unsubscribers, unsubscribers, unsubscribers_superset
):
    result = get_unsubscribed_emails_among(
        campaign_with_unsubscribers.account, unsubscribers_superset
    )
    assert len(result) == 3
    assert result == set(unsubscribers)


def test_among_unsubscribed_emails_superset_on_account_without_unsubscribers(
    campaign, unsubscribers_superset
):
    result = get_unsubscribed_emails_among(campaign.account, unsubscribers_superset)
    assert len(result) == 0
    assert result == set([])
