import pytest

from mail.payments.payments.core.actions.merchant.suggest import MerchantSuggestAction
from mail.payments.payments.interactions.spark_suggest.entities import SparkSuggestItem


class TestsMerchantSuggestAction:
    @pytest.fixture
    def query(self):
        return 'ИП Иванов 047...'

    @pytest.fixture
    def suggest_list(self):
        return [
            SparkSuggestItem(
                spark_id=i,
                name=f'suggested_name_{i}',
                full_name=f'suggested_full_name_{i}',
                inn=f'suggested_inn_{i}' if i != 2 else '',
                ogrn=f'suggested_ogrn_{i}',
                address=f'suggested_address_{i}',
                leader_name=f'suggested_leader_name_{i}',
                region_name=f'suggested_region_name_{i}',
            )
            for i in range(3)
        ]

    @pytest.fixture
    def spark_suggest_get_hint_mock(self, spark_suggest_client_mocker, suggest_list):
        with spark_suggest_client_mocker('get_hint', result=suggest_list) as mock:
            yield mock

    @pytest.fixture
    async def returned(self, query, spark_suggest_get_hint_mock):
        return await MerchantSuggestAction(query=query).run()

    def test_spark_suggest_get_hint_call(self, spark_suggest_get_hint_mock, query, returned):
        spark_suggest_get_hint_mock.assert_called_once_with(query=query)

    def test_result(self, suggest_list, returned):
        assert suggest_list[:2] == returned

    @pytest.mark.asyncio
    async def test_empty_request(self):
        assert [] == await MerchantSuggestAction(query='').run()
