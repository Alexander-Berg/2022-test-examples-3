import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectAPIError, EnvType
from aioresponses import aioresponses


class TestFeeds:
    async def test_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/feeds', status=200, payload={'result': {}})
            assert await direct.get_feeds('somebody', [12358]) is None

    async def test_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/feeds', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_feeds('somebody', [12358])
