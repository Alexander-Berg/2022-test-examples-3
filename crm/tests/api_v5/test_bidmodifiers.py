import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectAPIError, EnvType
from crm.supskills.common.direct_client.structs.structs.bidmodifiers import BidModifierGetItem, \
    DemographicsAdjustmentGet
from crm.supskills.common.direct_client.structs.enums.bidmodifiers import BidModifierLevelEnum, BidModifierTypeEnum, \
    AgeRangeEnum
from crm.supskills.common.direct_client.structs.enums.general import YesNoEnum
from aioresponses import aioresponses


class TestClients:
    async def test_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/bidmodifiers', status=200,
                        payload={'result': {'BidModifiers': [
                            {'CampaignId': 35, 'AdGroupId': None, 'Id': 113, 'Level': 'CAMPAIGN',
                             'Type': 'DEMOGRAPHICS_ADJUSTMENT', 'DemographicsAdjustment':
                                 {'Gender': None, 'Age': 'AGE_0_17', 'Enabled': 'YES'}}], 'LimitedBy': 1}})
            assert await direct.get_bidmodifiers('somebody', 12358) == \
                [BidModifierGetItem(CampaignId=35, Id=113, Level=BidModifierLevelEnum.CAMPAIGN,
                                    Type=BidModifierTypeEnum.DEMOGRAPHICS_ADJUSTMENT,
                                    DemographicsAdjustment=DemographicsAdjustmentGet(Age=AgeRangeEnum.AGE_0_17,
                                                                                     Enabled=YesNoEnum.YES))]

    async def test_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/bidmodifiers', status=200, payload={'result': {}})
            assert await direct.get_bidmodifiers('somebody', 12358) is None

    async def test_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/bidmodifiers', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_bidmodifiers('somebody', 12358)
