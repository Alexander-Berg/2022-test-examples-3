import pytest
from mail.payments.protos.spark_suggest_pb2 import HintRequest, HintResponse, SearchResult

from sendr_interactions.clients.spark_suggest import SparkSuggestClient
from sendr_interactions.clients.spark_suggest.entities import SparkSuggestItem


class TestSparkSuggestClient:
    @pytest.fixture
    def hint_data(self, rands, randn):
        return [
            {
                'id': randn(),
                'name': rands(),
                'full_name': rands(),
                'inn': rands(),
                'ogrn': rands(),
                'address': rands(),
                'leader_name': rands(),
                'region_name': rands()
            }
            for x in range(3)
        ]

    @pytest.fixture
    def hint_response(self, hint_data):
        response = HintResponse()
        for item in hint_data:
            search_result: SearchResult = response.values.add()

            search_result.id = item['id']
            search_result.name = item['name']
            search_result.full_name = item['full_name']
            search_result.inn = item['inn']
            search_result.ogrn = item['ogrn']
            search_result.address = item['address']
            search_result.leader_name = item['leader_name']
            search_result.region_name = item['region_name']
        return response

    @pytest.fixture
    def suggest_items(self, hint_data):
        return [
            SparkSuggestItem(
                spark_id=item['id'],
                name=item['name'],
                full_name=item['full_name'],
                inn=item['inn'],
                ogrn=item['ogrn'],
                address=item['address'],
                leader_name=item['leader_name'],
                region_name=item['region_name']
            )
            for item in hint_data
        ]

    @pytest.fixture
    async def spark_suggest_client(self, loop, mocker, dummy_logger, pushers_mock, hint_response):
        client = SparkSuggestClient(logger=dummy_logger, request_id='test', pushers=pushers_mock)
        mocker.patch.object(client, '_make_grpc_request', return_value=hint_response)
        return client

    @pytest.fixture
    def query(self, rands):
        return rands()

    @pytest.fixture
    def regions(self):
        return [1, 2, 3]

    @pytest.mark.asyncio
    async def test_request__generic(self, spark_suggest_client, query):
        await spark_suggest_client.get_hint(query=query)

        expected = HintRequest(query=query, count=10)
        spark_suggest_client._make_grpc_request.assert_called_once_with(expected)

    @pytest.mark.asyncio
    async def test_request__regions(self, spark_suggest_client, query, regions):
        await spark_suggest_client.get_hint(query=query, regions=regions)

        expected = HintRequest(query=query, count=10, regions=regions)
        spark_suggest_client._make_grpc_request.assert_called_once_with(expected)

    @pytest.mark.asyncio
    async def test_response__fill(self, query, spark_suggest_client, suggest_items):
        assert await spark_suggest_client.get_hint(query=query) == suggest_items

    @pytest.mark.parametrize('hint_response', (HintResponse(),))
    @pytest.mark.asyncio
    async def test_response__empty(self, query, spark_suggest_client):
        assert await spark_suggest_client.get_hint(query=query) == []
