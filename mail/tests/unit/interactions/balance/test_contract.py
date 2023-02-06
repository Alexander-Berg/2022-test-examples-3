import pytest

from hamcrest import assert_that, has_entries


class TestGetClientContracts:
    @pytest.fixture
    def contract_id(self, randn):
        return randn()

    @pytest.fixture
    def external_id(self):
        return 'test-get-client-contracts-external_id'

    @pytest.fixture
    def response_data(self, get_client_contracts_response):
        return get_client_contracts_response

    @pytest.mark.asyncio
    async def test_request(self, balance_client, client_id):
        await balance_client.get_client_contracts(client_id=client_id)
        assert balance_client.call_kwargs == {
            'method_name': 'GetClientContracts',
            'data': ({'ClientID': client_id},),
        }

    @pytest.mark.asyncio
    async def test_request_with_external_id(self, balance_client, client_id, external_id):
        await balance_client.get_client_contracts(client_id=client_id, external_id=external_id)
        assert balance_client.call_kwargs == {
            'method_name': 'GetClientContracts',
            'data': ({'ClientID': client_id, 'ExternalID': external_id},),
        }

    @pytest.mark.asyncio
    async def test_response(self, balance_client, client_id, person_id, contract_id, external_id):
        result = await balance_client.get_client_contracts(client_id=client_id, external_id=external_id)
        assert_that(
            result[0],
            has_entries({
                'ID': contract_id,
                'EXTERNAL_ID': external_id,
                'PERSON_ID': int(person_id),
            })
        )

    @pytest.mark.parametrize(['is_active', 'expected_len'], [[True, 1], [False, 0]])
    @pytest.mark.asyncio
    async def test_is_active_post_filter(self, balance_client, client_id, is_active, expected_len):
        result = await balance_client.get_client_contracts(client_id=client_id, is_active=is_active)
        assert len(result) == expected_len
