import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectAPIError, DirectKeyError, EnvType
from crm.supskills.common.direct_client.structs.structs.ads import AdGetItem, StatusEnum, AdSubtypeEnum
from aioresponses import aioresponses


class TestAds:
    async def test_get_ads_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/ads', status=200, payload={'result': {
                'Ads': [{'Id': 33, 'CampaignId': 12358, 'AdGroupId': 21, 'Subtype': 'NONE'},
                        {'Status': 'ACCEPTED', 'StatusClarification': 'Архивно.', 'AdCategories': None}]}})
            assert await direct.get_ads('somebody', 12358) == \
                   [AdGetItem(Id=33, CampaignId=12358, AdGroupId=21, Subtype=AdSubtypeEnum.NONE),
                    AdGetItem(Status=StatusEnum.ACCEPTED, StatusClarification='Архивно.')]

    async def test_get_ads_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/ads', status=200, payload={'result': {}})
            assert await direct.get_ads('somebody', 12358) is None

    async def test_get_ads_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/ads', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_ads('somebody', 12358)

    async def test_right_json_one_ad(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/ads', status=200, payload={'result': {
                'Ads': [{'Id': 33, 'CampaignId': 12358, 'AdGroupId': 21, 'Subtype': 'NONE'}]}})
            assert await direct.get_ad('somebody', 33) == \
                   AdGetItem(Id=33, CampaignId=12358, AdGroupId=21, Subtype=AdSubtypeEnum.NONE)

    async def test_get_ad_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/ads', status=200, payload={'result': {}})
            with pytest.raises(DirectKeyError):
                await direct.get_ad('somebody', 33)

    async def test_get_ad_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/ads', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_ad('somebody', 33)

    async def test_get_ads_by_ids_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/ads', status=200, payload={'result': {
                'Ads': [{'Id': 33, 'CampaignId': 12358, 'AdGroupId': 21, 'Subtype': 'NONE'},
                        {'Status': 'ACCEPTED', 'StatusClarification': 'Архивно.', 'AdCategories': None}]}})
            assert await direct.get_ads_by_ids('somebody', [33, 8]) == \
                   [AdGetItem(Id=33, CampaignId=12358, AdGroupId=21, Subtype=AdSubtypeEnum.NONE),
                    AdGetItem(Status=StatusEnum.ACCEPTED, StatusClarification='Архивно.')]

    async def test_get_ads_by_ids_empty_input(self):
        direct = Direct5(EnvType.tests, 'test_token')
        assert await direct.get_ads_by_ids('somebody', []) == []
