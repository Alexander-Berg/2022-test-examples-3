import pytest
from fan.testutils.pytest_fixtures import campaign
from fan.utils.cached_getters import (
    get_campaign_unsubscribe_list_exists,
    get_campaign_unsubscribe_lists_id,
)

pytestmark = pytest.mark.django_db

second_campaign = campaign


class TestUnsubCampaigns:
    """
    Тестирование определения кампаний для получения списков отписки
    """

    def test_no_unsub(self, campaign):
        campaign.unsubscribe_lists.clear()

        assert not get_campaign_unsubscribe_list_exists(campaign.id)


class TestCampaignUnsubLists:
    def test_unsub_lists_id(self, campaign, unsub_list, unsub_list_general):
        campaign.unsubscribe_lists = [unsub_list]

        unsub_ids = get_campaign_unsubscribe_lists_id(campaign.id)
        assert set(unsub_ids) == set([unsub_list.id, unsub_list_general.id])
