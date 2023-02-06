import pytest
from crm.supskills.common.direct_client.api_v5 import Direct5, DirectKeyError, DirectNetError, EnvType
from crm.supskills.common.direct_client.structs.structs.clients import ClientGetItem
from crm.supskills.common.direct_client.structs.enums.general import CurrencyEnum
from aioresponses import aioresponses


class TestClients:
    async def test_right_json(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/clients', status=200,
                        payload={'result': {'Clients': [{'AccountQuality': 5.6, 'ClientId': 261, 'CountryId': 225,
                                                         'CreatedAt': '2013-02-25', 'Currency': 'RUB'}]}})
            assert await direct.get_client('somebody') == \
                   ClientGetItem(AccountQuality=5.6, ClientId=261, CountryId=225,
                                 CreatedAt='2013-02-25', Currency=CurrencyEnum.RUB)

    async def test_broken_token_or_wrong_login(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/clients', status=200,
                        payload={"error": {"request_id": "2826", "error_detail": "Invalid OAuth token",
                                           "error_code": 53, "error_string": "Authorization error"}})
            with pytest.raises(DirectKeyError):
                await direct.get_client('somebody')

    async def test_timeout(self):
        with aioresponses() as mocked:
            direct = Direct5(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/json/v5/clients', status=522,
                        exception=TimeoutError('Connection timeout'))
            with pytest.raises(DirectNetError):
                await direct.get_client('somebody')
