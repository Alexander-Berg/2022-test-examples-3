from datetime import datetime
from unittest.mock import patch

from crm.supskills.common.direct_client.structs.general import from_dict
from crm.supskills.common.direct_client.structs.structs.campaigns import CampaignGetItem
from crm.supskills.direct_skill.src.intents.show_conditions.show_absent import get_local_time, is_now_work_hour, \
    is_campaign_started, is_strategy_with_search, to_moscow_time


async def test_def_is_strategy_with_search():
    campaign = from_dict(CampaignGetItem, {'Type': 'TEXT_CAMPAIGN', 'TextCampaign': {
        'BiddingStrategy': {'Search': {'BiddingStrategyType': 'SERVING_OFF'}}}})
    assert is_strategy_with_search(campaign) is False
    campaign = from_dict(CampaignGetItem, {'Type': 'SMART_CAMPAIGN', 'SmartCampaign': {
        'BiddingStrategy': {'Search': {'BiddingStrategyType': 'SERVING_OFF'}}}})
    assert is_strategy_with_search(campaign) is False
    campaign = from_dict(CampaignGetItem, {'Type': 'DYNAMIC_TEXT_CAMPAIGN', 'DynamicTextCampaign': {
        'BiddingStrategy': {'Search': {'BiddingStrategyType': 'SERVING_OFF'}}}})
    assert is_strategy_with_search(campaign) is False
    campaign = from_dict(CampaignGetItem, {'Type': 'MOBILE_APP_CAMPAIGN', 'MobileAppCampaign': {
        'BiddingStrategy': {'Search': {'BiddingStrategyType': 'SERVING_OFF'}}}})
    assert is_strategy_with_search(campaign) is False
    campaign = from_dict(CampaignGetItem, {'Type': 'CPM_BANNER_CAMPAIGN', 'CpmBannerCampaign': {
        'BiddingStrategy': {'Search': {'BiddingStrategyType': 'SERVING_OFF'}}}})
    assert is_strategy_with_search(campaign) is False
    campaign = from_dict(CampaignGetItem, {'Type': 'TEXT_CAMPAIGN', 'TextCampaign': {
        'BiddingStrategy': {'Search': {'BiddingStrategyType': 'PAY_FOR_CONVERSION'}}}})
    assert is_strategy_with_search(campaign) is True
    campaign = from_dict(CampaignGetItem, {'Type': 'SMART_CAMPAIGN', 'SmartCampaign': {
        'BiddingStrategy': {'Search': {'BiddingStrategyType': 'UNKNOWN'}}}})
    assert is_strategy_with_search(campaign) is True


async def test_local_time():
    time_zones = ['Africa/Blantyre', 'Africa/Bujumbura', 'Africa/Cairo', 'Africa/Gaborone', 'Africa/Harare',
                  'Africa/Johannesburg', 'Africa/Kigali', 'Africa/Lubumbashi', 'Africa/Lusaka', 'Africa/Maputo',
                  'Africa/Maseru', 'Africa/Mbabane', 'Africa/Tripoli', 'Asia/Amman', 'Asia/Beirut', 'Asia/Damascus',
                  'Asia/Gaza', 'Asia/Istanbul', 'Asia/Jerusalem', 'Asia/Nicosia', 'Asia/Tel_Aviv', 'EET',
                  'Egypt', 'Etc/GMT-2', 'Europe/Athens', 'Europe/Bucharest', 'Europe/Chisinau', 'Europe/Helsinki',
                  'Europe/Istanbul', 'Europe/Kaliningrad', 'Europe/Kiev', 'Europe/Mariehamn', 'Europe/Minsk',
                  'Europe/Nicosia', 'Europe/Riga', 'Europe/Simferopol', 'Europe/Sofia', 'Europe/Tallinn',
                  'Europe/Tiraspol', 'Europe/Uzhgorod', 'Europe/Vilnius', 'Europe/Zaporozhye', 'Israel',
                  'Libya', 'Turkey', 'Europe/Moscow']  # 'ART', 'CAT',
    for tz in time_zones:
        get_local_time(tz)
    assert str(to_moscow_time(datetime.strptime(
        '2021-9-16', "%Y-%m-%d"))) == '2021-09-16 00:00:00+03:00'


async def test_def_is_now_work_hour():
    with patch('crm.supskills.direct_skill.src.intents.show_conditions.show_absent.datetime') as dt_mock:
        dt_mock.utcnow.return_value = datetime(2021, 9, 19, 21, 30, 0)
        campaign = from_dict(CampaignGetItem, {'TimeTargeting': {'Schedule': {
            'Items': ['1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '5,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '6,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '7,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1']}}, 'TimeZone': 'Europe/Moscow'})
        assert is_now_work_hour(campaign) is False
        dt_mock.utcnow.return_value = datetime(2021, 9, 19, 20, 30, 0)
        campaign = from_dict(CampaignGetItem, {'TimeTargeting': {'Schedule': {
            'Items': ['1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '3,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '5,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '6,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1',
                      '7,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0']}}, 'TimeZone': 'Europe/Moscow'})
        assert is_now_work_hour(campaign) is False

        dt_mock.utcnow.return_value = datetime(2021, 9, 19, 20, 30, 0)
        campaign = from_dict(CampaignGetItem, {'TimeTargeting': {'Schedule': {
            'Items': []}}, 'TimeZone': 'Europe/Moscow'})
        assert is_now_work_hour(campaign) is True


async def test_def_is_campaign_started():
    with patch('crm.supskills.direct_skill.src.intents.show_conditions.show_absent.datetime') as dt_mock:
        dt_mock.utcnow.return_value = datetime(2021, 7, 29, 21, 0, 0)
        dt_mock.strptime.return_value = datetime(2021, 7, 30)
        campaign = from_dict(CampaignGetItem, {'TimeZone': 'Europe/Moscow'})
        assert is_campaign_started(campaign) is False

        dt_mock.utcnow.return_value = datetime(2021, 7, 29, 21, 0, 1)
        dt_mock.strptime.return_value = datetime(2021, 7, 30)
        campaign = from_dict(CampaignGetItem, {'TimeZone': 'Europe/Moscow'})
        assert is_campaign_started(campaign) is True
