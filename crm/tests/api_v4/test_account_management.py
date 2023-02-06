import pytest
from crm.supskills.common.direct_client.api_v4 import Direct4, DirectAPIError, EnvType
from crm.supskills.common.direct_client.structs.structs.account_management import Account, CurrencyEnum
from aioresponses import aioresponses


class TestAccountManagement:
    async def test_right_json(self):
        with aioresponses() as mocked:
            direct = Direct4(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/live/v4/json/', status=200, payload={
                "data": {"Accounts": [
                    {"SmsNotification": None, "AccountID": 47, "Currency": "RUB", "Amount": "30.5674",
                     "Login": "somebody", "AmountAvailableForTransfer": "300"}], "ActionsResult": []}})
            assert await direct.get_account_management('somebody') == \
                   [Account(AccountID=47, Currency=CurrencyEnum.RUB, Amount=30.5674, Login='somebody',
                            AmountAvailableForTransfer=300)]

    async def test_empty_json(self):
        with aioresponses() as mocked:
            direct = Direct4(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/live/v4/json/', status=200, payload={"data": {}})
            assert await direct.get_account_management('somebody') is None

    async def test_broken_token(self):
        with aioresponses() as mocked:
            direct = Direct4(EnvType.tests, 'test_token')
            mocked.post('https://ipv6.api.direct.yandex.ru/live/v4/json/', status=200,
                        payload={"error": {"request_id": "2826", "error_code": 53,
                                           "error_detail": "Invalid OAuth token",
                                           "error_string": "Authorization error"}})
            with pytest.raises(DirectAPIError):
                await direct.get_account_management('somebody')
