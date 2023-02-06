import re
from xmlrpc import client as xmlrpc

import pytest

from hamcrest import assert_that, equal_to, has_entries


class TestGetClientContracts:
    @pytest.fixture
    def contract_id(self):
        return 999999999

    @pytest.fixture
    def external_id(self):
        return 'test-get-client-contracts-external_id'

    @pytest.mark.asyncio
    async def test_request(self, balance_client, client_id, balance_mock):
        await balance_client.get_client_contracts(client_id=client_id)
        assert_that(
            xmlrpc.loads(balance_mock.call_args.kwargs['data']),
            equal_to(
                (
                    ({'ClientID': client_id},),
                    'GetClientContracts',
                )
            )
        )

    @pytest.mark.asyncio
    async def test_request_with_external_id(self, balance_client, client_id, external_id, balance_mock):
        await balance_client.get_client_contracts(client_id=client_id, external_id=external_id)
        assert_that(
            xmlrpc.loads(balance_mock.call_args.kwargs['data']),
            equal_to(
                (
                    ({'ClientID': client_id, 'ExternalID': external_id},),
                    'GetClientContracts',
                )
            )
        )

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

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, get_client_contracts_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=get_client_contracts_response)
