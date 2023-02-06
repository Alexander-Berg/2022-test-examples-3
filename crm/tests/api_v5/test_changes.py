import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectAPIError, EnvType
from crm.supskills.common.direct_client.structs.structs.changes import CheckResponseModified
from aioresponses import aioresponses


class TestChanges:
    async def test_get_changed_in_campaign_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/changes', status=200,
                        payload={'result': {'Modified': {}, 'Timestamp': '2021-07-21T18:01:57Z'}})
            assert await direct.get_changes_in_campaign('somebody', 12358, '2020-07-21T18:01:57Z') == \
                   CheckResponseModified()

    async def test_get_changed_in_campaign_simple_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/changes', status=200,
                        payload={'result': {'Modified': {'AdIds': [3307544853, 3307544855],
                                                         'AdGroupIds': [2198462121, 2198462122],
                                                         'CampaignIds': [23200301]},
                                            'Timestamp': '2021-07-21T18:07:34Z'}})
            assert await direct.get_changes_in_campaign('somebody', 12358, '2020-07-21T18:01:57Z') == \
                   CheckResponseModified(AdIds=[3307544853, 3307544855],
                                         AdGroupIds=[2198462121, 2198462122], CampaignIds=[23200301])

    async def test_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/changes', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_changes_in_campaign('somebody', 12358, '2020-07-21T18:01:57Z')
