import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectAPIError, EnvType
from crm.supskills.common.direct_client.structs.structs.ad_groups import AdGroupGetItem, StatusEnum
from crm.supskills.common.direct_client.structs.structs.general import ArrayOfString
from aioresponses import aioresponses


class TestAdGroups:
    async def test_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/adgroups', status=200, payload={
                'result': {'AdGroups': [{'Id': 2198462121, 'Name': 'тест', 'CampaignId': 12358,
                                         'Status': 'ACCEPTED', 'NegativeKeywords': {'Items': ['!как']},
                                         'RegionIds': [17, -10842]}]}})
            assert await direct.get_ad_groups('somebody', 12358) == [
                AdGroupGetItem(Id=2198462121, Name='тест', CampaignId=12358, Status=StatusEnum.ACCEPTED,
                               NegativeKeywords=ArrayOfString(Items=['!как']), RegionIds=[17, -10842])]

    async def test_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/adgroups', status=200, payload={'result': {}})
            assert await direct.get_ad_groups('somebody', 12358) is None

    async def test_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/adgroups', status=200,
                        payload={"error": {"request_id": "2826", "error_code": 53,
                                           "error_detail": "Invalid OAuth token",
                                           "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_ad_groups('somebody', 12358)

    async def test_get_ad_groups_by_ids_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/adgroups', status=200, payload={
                'result': {'AdGroups': [{'Id': 262121, 'Name': 'тест', 'CampaignId': 12358,
                                         'Status': 'ACCEPTED', 'NegativeKeywords': {'Items': ['!как']},
                                         'RegionIds': [17, -10842]}]}})
            assert await direct.get_ad_groups_by_ids('somebody', [262121]) == [
                AdGroupGetItem(Id=262121, Name='тест', CampaignId=12358, Status=StatusEnum.ACCEPTED,
                               NegativeKeywords=ArrayOfString(Items=['!как']), RegionIds=[17, -10842])]

    async def test_get_ad_groups_by_ids_empty_input(self):
        direct = Direct5(EnvType.tests, 'test_token')
        assert await direct.get_ad_groups_by_ids('somebody', []) == []
