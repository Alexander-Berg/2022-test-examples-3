import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectAPIError, DirectKeyError, EnvType
from crm.supskills.common.direct_client.structs.structs.campaigns import CampaignGetItem, StatisticsClass, \
    CampaignTypeGetEnum, CampaignStateGetEnum, StatusEnum
from aioresponses import aioresponses


class TestCampaigns:
    async def test_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={'result': {'LimitedBy': 1,
                                            'Campaigns': [
                                                {'StatusClarification': 'Идут показы', 'TimeZone': 'Europe/Moscow',
                                                 'Statistics': {'Clicks': 0, 'Impressions': 0},
                                                 'Status': 'ACCEPTED', 'Type': 'TEXT_CAMPAIGN', 'State': 'ON',
                                                 'ExcludedSites': None}]}})
            assert await direct.get_campaign('somebody', 12358) == \
                   CampaignGetItem(StatusClarification='Идут показы', Type=CampaignTypeGetEnum.TEXT_CAMPAIGN,
                                   Statistics=StatisticsClass(Clicks=0, Impressions=0), TimeZone='Europe/Moscow',
                                   State=CampaignStateGetEnum.ON, Status=StatusEnum.ACCEPTED)

    async def test_get_wrong_id(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200, payload={'result': {'Campaigns': []}})
            with pytest.raises(DirectKeyError):
                await direct.get_campaign('somebody', 12358)

    async def test_get_single_campaign_id_ok(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={'result': {'LimitedBy': 1, 'Campaigns': [{'Type': 'TEXT_CAMPAIGN', 'Id': 239}]}})
            assert await direct.get_single_campaign('somebody') == \
                   CampaignGetItem(Type=CampaignTypeGetEnum.TEXT_CAMPAIGN, Id=239)

    async def test_get_single_campaign_id_not_one(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={'result': {'LimitedBy': 2, 'Campaigns': [{'Id': 239}, {'Id': 30}]}})
            with pytest.raises(DirectKeyError):
                await direct.get_single_campaign('somebody')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200, payload={'result': {}})
            with pytest.raises(DirectKeyError):
                await direct.get_single_campaign('somebody')

    async def test_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_single_campaign('somebody')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_campaign('somebody', 12358)
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_campaigns('somebody')

    async def test_get_campaigns_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={'result': {'Campaigns': [{'Id': 239}, {'Id': 30}]}})
            assert await direct.get_campaigns('somebody') == [CampaignGetItem(Id=239), CampaignGetItem(Id=30)]

    async def test_get_campaigns_by_ids_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/campaigns', status=200,
                        payload={'result': {'Campaigns': [{'Id': 239}, {'Id': 30}]}})
            assert await direct.get_campaigns_by_ids('somebody', [239, 30]) == \
                   [CampaignGetItem(Id=239), CampaignGetItem(Id=30)]

    async def test_get_campaigns_by_ids_empty_input(self):
        direct = Direct5(EnvType.tests, 'test_token')
        assert await direct.get_campaigns_by_ids('somebody', []) == []
