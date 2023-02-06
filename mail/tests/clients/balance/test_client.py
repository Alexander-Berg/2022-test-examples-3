import re
from datetime import date
from xmlrpc import client as xmlrpc

import pytest

from sendr_interactions.clients.balance.entities import Client
from sendr_interactions.clients.balance.exceptions import BalanceUserAlreadyLinkedToClient, BalanceUserNotLinkedToClient

from hamcrest import assert_that, equal_to, match_equality, not_none


class TestCreateClient:
    @pytest.fixture
    def client_entity(self):
        return Client(
            name='test-create-client-name',
            email='test-create-client-email',
            phone='test-create-client-phone',
        )

    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, client_entity, balance_mock):
        await balance_client.create_client(uid=333, client=client_entity)
        balance_mock.assert_called_once_with(
            data=xmlrpc.dumps(
                (
                    str(333),
                    {
                        'NAME': client_entity.name,
                        'EMAIL': client_entity.email,
                        'PHONE': client_entity.phone,
                    },
                ),
                'CreateClient',
            ),
            headers=match_equality(not_none()),
        )

    @pytest.mark.asyncio
    async def test_returned(self, balance_client, client_entity, client_id):
        returned = await balance_client.create_client(uid=333, client=client_entity)
        assert returned == client_id

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, create_client_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=create_client_response)


class TestCreateUserClientAssociation:
    @pytest.fixture
    def response_data(self, user_client_association_response):
        return user_client_association_response

    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, balance_mock, client_id):
        await balance_client.create_user_client_association(uid=333, client_id=client_id)
        balance_mock.assert_called_once_with(
            data=xmlrpc.dumps(
                (str(333), client_id, str(333)),
                'CreateUserClientAssociation',
            ),
            headers=match_equality(not_none()),
        )

    class TestAlreadyLinked:
        @pytest.fixture
        def response_code(self):
            return 4008  # balance code for user being already linked to client

        @pytest.mark.asyncio
        async def test_raises(self, balance_client, client_id):
            with pytest.raises(BalanceUserAlreadyLinkedToClient):
                await balance_client.create_user_client_association(uid=333, client_id=client_id)

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, user_client_association_response):
        return aioresponses_mocker.post(
            re.compile(rf'{balance_client.BASE_URL}'), body=user_client_association_response
        )


class TestRemoveUserClientAssociation:
    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, client_id, balance_mock):
        await balance_client.remove_user_client_association(uid=333, client_id=client_id)
        balance_mock.assert_called_once_with(
            data=xmlrpc.dumps(
                (str(333), client_id, str(333)),
                'RemoveUserClientAssociation',
            ),
            headers=match_equality(not_none()),
        )

    class TestNotLinked:
        @pytest.fixture
        def response_code(self):
            return 4009  # balance code for user not being linked to client

        @pytest.mark.asyncio
        async def test_raises(self, balance_client, client_id):
            with pytest.raises(BalanceUserNotLinkedToClient):
                await balance_client.remove_user_client_association(uid=333, client_id=client_id)

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, user_client_association_response):
        return aioresponses_mocker.post(
            re.compile(rf'{balance_client.BASE_URL}'), body=user_client_association_response
        )


class TestFindClient:
    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, balance_mock):
        await balance_client.find_client(uid=333)
        balance_mock.assert_called_once_with(
            data=xmlrpc.dumps(
                ({'PassportID': str(333)},),
                'FindClient',
            ),
            headers=match_equality(not_none()),
        )

    @pytest.mark.asyncio
    async def test_returns_client(self, balance_client, client_entity):
        returned = await balance_client.find_client(uid=333)

        assert returned == client_entity

    class TestNoClient:
        @pytest.fixture
        def client_id(self):
            return None

        @pytest.mark.asyncio
        async def test_returns_none(self, balance_client):
            returned = await balance_client.find_client(uid=333)

            assert returned is None

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, find_client_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=find_client_response)


class TestLinkIntegrationToClient:
    @pytest.fixture
    def params(self):
        return {
            'uid': 333,
            'client_id': '1',
            'integration_cc': 'integration-cc',
            'configuration_cc': 'configuration-cc',
            'start_date': date(2000, 1, 1),
        }

    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, balance_mock, params):
        await balance_client.link_integration_to_client(**params)

        assert_that(
            xmlrpc.loads(balance_mock.call_args.kwargs['data']),
            equal_to(
                (
                    (
                        str(333),
                        {
                            'ClientID': '1',
                            'IntegrationCC': 'integration-cc',
                            'ConfigurationCC': 'configuration-cc',
                            'StartDate': '2000-01-01',
                        },
                    ),
                    'LinkIntegrationToClient',
                )
            )
        )

    @pytest.mark.asyncio
    async def test_without_optionals(self, balance_client, balance_mock, params):
        params.pop('start_date')

        await balance_client.link_integration_to_client(**params)

        assert_that(
            xmlrpc.loads(balance_mock.call_args.kwargs['data']),
            equal_to(
                (
                    (
                        str(333),
                        {
                            'ClientID': '1',
                            'IntegrationCC': 'integration-cc',
                            'ConfigurationCC': 'configuration-cc',
                        },
                    ),
                    'LinkIntegrationToClient',
                )
            )
        )

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, balance_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=balance_response)

    @pytest.fixture
    def balance_response(self):
        return """
            <methodResponse>
            <params>
            <param>
            <value><int>0</int></value>
            </param>
            <param>
            <value><string>Some message</string></value>
            </param>
            </params>
            </methodResponse>
        """
