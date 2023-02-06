from unittest.mock import AsyncMock

import pytest
from crm.supskills.common.direct_client.api_v4 import Direct4, DirectNetError, DirectKeyError, DirectAPIError
from crm.supskills.common.direct_client.api_v5 import Direct5
from crm.supskills.common.direct_client.structs.enums.ad_groups import AdGroupSubtypeEnum
from crm.supskills.common.direct_client.structs.structs.account_management import Account
from crm.supskills.common.direct_client.structs.structs.ad_groups import AdGroupGetItem, SmartAdGroupGet, \
    DynamicTextFeedAdGroupGet
from crm.supskills.common.direct_client.structs.structs.ads import AdGetItem
from crm.supskills.common.direct_client.structs.structs.campaigns import CampaignGetItem, TextCampaignSearchStrategy, \
    TimeTargetingClass, TextCampaignGetItem, TextCampaignStrategy
from crm.supskills.common.direct_client.structs.structs.feeds import FeedGetItem
from crm.supskills.common.direct_client.structs.structs.general import ArrayOfString
from crm.supskills.direct_skill.src.core.models.conversation import Conversation
from crm.supskills.direct_skill.src.intents.general.simple_direct_intent import SimpleDirectIntent


@pytest.fixture
def fake_bunker():
    class FakeBunker:
        @staticmethod
        async def get_node(path, node, *args, **kwargs):
            return path, node

    return FakeBunker


@pytest.fixture
def direct5_campaign_status_archived():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="ARCHIVED"))
    return Direct5Test()


@pytest.fixture
def direct5_campaign_status_on():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="ON"))
    return Direct5Test()


@pytest.fixture
def direct5_campaign_status_draft():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="OFF", Status="DRAFT"))
    return Direct5Test()


@pytest.fixture
def direct5_campaign_status_suspended():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="SUSPENDED"))
    return Direct5Test()


@pytest.fixture
def direct5_campaign_status_moderation():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="OFF", Status="MODERATION"))
    return Direct5Test()


@pytest.fixture
def direct5_campaign_status_rejected():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="OFF", Status="REJECTED"))
    return Direct5Test()


@pytest.fixture
def direct5_campaign_status_no_shows():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="OFF", Status="ACCEPTED"))
    return Direct5Test()


@pytest.fixture
def direct4_campaign_status_no_shows():
    class Direct4Test(Direct4):
        get_account_management = AsyncMock(return_value=[Account(Amount=-346364.435346)])
    return Direct4Test()


@pytest.fixture
def direct5_campaign_status_rarely_served():
    class Direct5Test(Direct5):
        get_campaigns_by_ids = AsyncMock(return_value=[CampaignGetItem(Id=13, State="UNKNOWN")])
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_ad_groups = AsyncMock(return_value=[AdGroupGetItem(ServingStatus="RARELY_SERVED"),
                                                AdGroupGetItem(ServingStatus="RARELY_SERVED")])
    return Direct5Test()


@pytest.fixture
def direct5_campaign_status_not_created():
    class Direct5Test(Direct5):
        get_feeds = AsyncMock(return_value=[FeedGetItem(Status="NEW"), FeedGetItem(Status="ERROR")])
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(Id=13, State="UNKNOWN"))
        get_ad_groups = AsyncMock(return_value=[
            AdGroupGetItem(Type="SMART_AD_GROUP", ServingStatus="ELIGIBLE", SmartAdGroup=SmartAdGroupGet(FeedId=115)),
            AdGroupGetItem(Type="DYNAMIC_TEXT_AD_GROUP", ServingStatus="RARELY_SERVED", Subtype=AdGroupSubtypeEnum.FEED,
                           DynamicTextFeedAdGroup=DynamicTextFeedAdGroupGet(FeedId=120))])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_one_group_no_ads():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaign = AsyncMock(return_value=CampaignGetItem(Id=239, State="ARCHIVED"))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[AdGroupGetItem(CampaignId=239)])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_many_groups_no_ads():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[AdGroupGetItem(CampaignId=239), AdGroupGetItem(CampaignId=23)])
        get_ads_by_ids = AsyncMock(return_value=[])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_many_groups_one_ad():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[AdGroupGetItem(CampaignId=239), AdGroupGetItem(CampaignId=23)])
        get_ads_by_ids = AsyncMock(return_value=[AdGetItem(CampaignId=239)])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_one_group_same_ad():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaign = AsyncMock(return_value=CampaignGetItem(Id=239, State="ARCHIVED"))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[AdGroupGetItem(CampaignId=239)])
        get_ads_by_ids = AsyncMock(return_value=[AdGetItem(CampaignId=239)])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_one_group_different_ad():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaign = AsyncMock(return_value=CampaignGetItem(Id=239, State="ARCHIVED"))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[AdGroupGetItem(CampaignId=239)])
        get_ads_by_ids = AsyncMock(return_value=[AdGetItem(CampaignId=249)])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_no_group_one_ad():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaign = AsyncMock(return_value=CampaignGetItem(Id=239, State="ARCHIVED"))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[])
        get_ads_by_ids = AsyncMock(return_value=[AdGetItem(CampaignId=239)])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_no_group_many_ads():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaign = AsyncMock(return_value=CampaignGetItem(Id=239, State="ARCHIVED"))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[])
        get_ads_by_ids = AsyncMock(return_value=[AdGetItem(CampaignId=239), AdGetItem(CampaignId=29)])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_start_later():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaigns_by_ids = AsyncMock(return_value=[
            CampaignGetItem(Id=13, State="ON", StartDate="2050-7-31", TimeZone="Europe/Moscow")])
        get_ad_groups_by_ids = AsyncMock(return_value=[])
        get_ads_by_ids = AsyncMock(return_value=[])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_not_work_time():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaigns_by_ids = AsyncMock(return_value=[
            CampaignGetItem(Id=13, State="ON", StartDate="2021-7-31", TimeZone="Europe/Moscow",
                            TimeTargeting=TimeTargetingClass(Schedule=ArrayOfString(Items=[
                                "1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1",
                                "2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1",
                                "3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1",
                                "4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1",
                                "5,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1",
                                "6,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1",
                                "7,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"
                            ])))])
        get_ad_groups_by_ids = AsyncMock(return_value=[])
        get_ads_by_ids = AsyncMock(return_value=[])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_not_rarely_served():
    class Direct5Test(Direct5):
        get_ad_groups = AsyncMock(return_value=[AdGroupGetItem(ServingStatus="RARELY_SERVED"),
                                                AdGroupGetItem(ServingStatus="RARELY_SERVED")])
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(
            Id=13, State="ON", StartDate="2021-7-31", TimeZone="Europe/Moscow",
            TimeTargeting=TimeTargetingClass(Schedule=ArrayOfString(Items=[
                "5,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0"]))))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[])
        get_ads_by_ids = AsyncMock(return_value=[])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_is_search():
    class Direct5Test(Direct5):
        get_ad_groups = AsyncMock(return_value=[AdGroupGetItem(ServingStatus="ELIGIBLE"),
                                                AdGroupGetItem(ServingStatus="RARELY_SERVED")])
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(
            Id=13, State="ON", StartDate="2021-7-31", TimeZone="Europe/Moscow", Type="TEXT_CAMPAIGN",
            TextCampaign=TextCampaignGetItem(
                BiddingStrategy=TextCampaignStrategy(Search=TextCampaignSearchStrategy(BiddingStrategyType="AVERAGE_CPC"))
            ),
            TimeTargeting=TimeTargetingClass(Schedule=ArrayOfString(Items=[
                "5,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0"]))))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[])
        get_ads_by_ids = AsyncMock(return_value=[])
    return Direct5Test()


@pytest.fixture
def direct5_show_absent_is_network():
    class Direct5Test(Direct5):
        get_ad_groups = AsyncMock(return_value=[AdGroupGetItem(ServingStatus="ELIGIBLE"),
                                                AdGroupGetItem(ServingStatus="RARELY_SERVED")])
        get_single_campaign = AsyncMock(return_value=CampaignGetItem(
            Id=13, State="ON", StartDate="2021-7-31", TimeZone="Europe/Moscow", Type="TEXT_CAMPAIGN",
            TextCampaign=TextCampaignGetItem(
                BiddingStrategy=TextCampaignStrategy(Search=TextCampaignSearchStrategy(BiddingStrategyType="SERVING_OFF"))
            ),
            TimeTargeting=TimeTargetingClass(Schedule=ArrayOfString(Items=[
                "5,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0"]))))
        get_campaigns_by_ids = AsyncMock(return_value=[])
        get_ad_groups_by_ids = AsyncMock(return_value=[])
        get_ads_by_ids = AsyncMock(return_value=[])
    return Direct5Test()


@pytest.fixture
def direct4_ok():
    class Direct4Test(Direct4):
        get_single_campaign = AsyncMock()
        get_campaigns_by_ids = AsyncMock()
        get_ad_groups_by_ids = AsyncMock()
        get_ads_by_ids = AsyncMock()
    return Direct4Test()


@pytest.fixture
def direct5_key_error():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock(side_effect=DirectKeyError)
        get_campaigns_by_ids = AsyncMock(side_effect=DirectKeyError)
        get_ad_groups_by_ids = AsyncMock(side_effect=DirectKeyError)
        get_ads_by_ids = AsyncMock(side_effect=DirectKeyError)
    return Direct5Test()


@pytest.fixture
def direct5_api_error():
    class Direct5Test(Direct5):
        _make_request = AsyncMock(side_effect=DirectAPIError)
    return Direct5Test()


@pytest.fixture
def direct5_net_error():
    class Direct5Test(Direct5):
        __send_request = AsyncMock(side_effect=DirectNetError)
    return Direct5Test()


@pytest.fixture
def direct4_net_error():
    class Direct4Test(Direct4):
        __send_request = AsyncMock(side_effect=DirectNetError)
    return Direct4Test()


@pytest.fixture
def SimpleDirectIntent_no_login():
    class SimpleDirectIntentTest(SimpleDirectIntent):
        @staticmethod
        def get_login(self):
            return None

    return SimpleDirectIntentTest(fake_bunker)


@pytest.fixture
def direct5_ok():
    class Direct5Test(Direct5):
        get_single_campaign = AsyncMock()
        get_campaigns_by_ids = AsyncMock()
        get_ad_groups_by_ids = AsyncMock()
        get_ads_by_ids = AsyncMock()
    return Direct5Test()


@pytest.fixture
def conversation_no_login():
    return Conversation({'session': {
        'floyd_user': {'operator_chat_id': 'Some oper Id'}},
        'state': {'session': {'state': '', 'empty_intents_count': 0}},
        'request': {
            'nlu': {
                'tokens': ['привет'],
                'intents': {'show_absent': {'slots': {}}},
                'entities': []},
            'original_utterance': 'Вообще не важно что тут! В 2к21 было :)'}}, {},
        {'state': '', 'empty_intents_count': 0})


@pytest.fixture
def conversation_with_login():
    return Conversation({'session': {
        'floyd_user': {'login': 'FakeLogin', 'operator_chat_id': 'Some oper Id'}},
        'state': {'session': {'state': '', 'empty_intents_count': 0}},
        'request': {
            'nlu': {
                'tokens': ['привет'],
                'intents': {'show_absent': {'slots': {}}},
                'entities': []},
            'original_utterance': 'Вообще не важно что тут! В 2к21 было :)'}}, {},
        {'state': '', 'empty_intents_count': 0})
