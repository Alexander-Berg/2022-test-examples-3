import pytest
from django.core.exceptions import MultipleObjectsReturned
from django.db import transaction, OperationalError
from fan.campaigns.get import get_locked_campaign_by_slug
from fan.models import Campaign


pytestmark = pytest.mark.django_db


@pytest.mark.django_db(transaction=True)
def test_double_lock_fails(campaign):
    with pytest.raises(OperationalError):
        with transaction.atomic():
            locked_campaign = get_locked_campaign_by_slug(campaign.slug, campaign.account.id)
            with transaction.atomic(using="alternate"):
                locked_campaign2 = get_locked_campaign_by_slug(
                    campaign.slug, campaign.account.id, using="alternate"
                )


@pytest.mark.django_db(transaction=True)
def test_lock_success(campaign):
    with transaction.atomic():
        locked_campaign = get_locked_campaign_by_slug(campaign.slug, campaign.account.id)
        assert locked_campaign.id == campaign.id


def test_getting_unexisted_campaign_raises(campaign):
    with pytest.raises(Campaign.DoesNotExist):
        get_locked_campaign_by_slug("unexisted_campaign", campaign.account.id)


def test_getting_multiple_results_raises(campaign, multiple_query_result):
    with pytest.raises(MultipleObjectsReturned):
        get_locked_campaign_by_slug("multiple_results", campaign.account.id)


@pytest.fixture
def multiple_query_result(mocker):
    mocker.patch("django.db.models.query.QuerySet.__len__", return_value=2)
