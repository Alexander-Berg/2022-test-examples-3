import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectAPIError, DirectKeyError, EnvType
from crm.supskills.common.direct_client.structs.structs.turbopages import TurboPageGetItem
from aioresponses import aioresponses


class TestTurbopages:
    async def test_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/turbopages', status=200,
                        payload={'result': {'TurboPages': [{'Id': 103, 'Name': 'Быстрый старт',
                                                            'Href': 'https://yandex.ru/'}]}})
            assert await direct.get_turbopage('somebody', 103) == \
                   TurboPageGetItem(Id=103, Name='Быстрый старт', Href='https://yandex.ru/')

    async def test_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/turbopages', status=200, payload={'result': {}})
            with pytest.raises(DirectKeyError):
                await direct.get_turbopage('somebody', 103)

    async def test_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/turbopages', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_turbopage('somebody', 103)
