import pytest

from mail.payments.payments.core.entities.client import Client
from mail.payments.payments.interactions.balance.exceptions import (
    BalanceUserAlreadyLinkedToClient, BalanceUserNotLinkedToClient
)


class TestCreateClient:
    @pytest.fixture
    def client_entity(self):
        return Client(
            name='test-create-client-name',
            email='test-create-client-email',
            phone='test-create-client-phone',
        )

    @pytest.fixture
    def response_data(self, create_client_response):
        return create_client_response

    @pytest.fixture
    async def returned(self, balance_client, merchant_uid, client_entity):
        return await balance_client.create_client(merchant_uid, client_entity)

    def test_request_call(self, balance_client, merchant_uid, client_entity, returned):
        assert balance_client.call_kwargs == {
            'method_name': 'CreateClient',
            'data': (
                str(merchant_uid),
                {
                    'NAME': client_entity.name,
                    'EMAIL': client_entity.email,
                    'PHONE': client_entity.phone,
                },
            ),
        }

    def test_returned(self, client_id, returned):
        assert returned == client_id


class TestCreateUserClientAssociation:
    @pytest.fixture
    def response_data(self, user_client_association_response):
        return user_client_association_response

    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, merchant_uid, client_id):
        await balance_client.create_user_client_association(merchant_uid, client_id)
        assert balance_client.call_kwargs == {
            'method_name': 'CreateUserClientAssociation',
            'data': (str(merchant_uid), client_id, str(merchant_uid)),
        }

    class TestAlreadyLinked:
        @pytest.fixture
        def response_code(self):
            return 4008  # balance code for user being already linked to client

        @pytest.mark.asyncio
        async def test_raises(self, balance_client, merchant_uid, client_id):
            with pytest.raises(BalanceUserAlreadyLinkedToClient):
                await balance_client.create_user_client_association(merchant_uid, client_id)


class TestRemoveUserClientAssociation:
    @pytest.fixture
    def response_data(self, user_client_association_response):
        return user_client_association_response

    @pytest.mark.asyncio
    async def test_request_call(self, balance_client, merchant_uid, client_id):
        await balance_client.remove_user_client_association(merchant_uid, client_id)
        assert balance_client.call_kwargs == {
            'method_name': 'RemoveUserClientAssociation',
            'data': (str(merchant_uid), client_id, str(merchant_uid)),
        }

    class TestNotLinked:
        @pytest.fixture
        def response_code(self):
            return 4009  # balance code for user not being linked to client

        @pytest.mark.asyncio
        async def test_raises(self, balance_client, merchant_uid, client_id):
            with pytest.raises(BalanceUserNotLinkedToClient):
                await balance_client.remove_user_client_association(merchant_uid, client_id)


class TestFindClient:
    @pytest.fixture
    def response_data(self, find_client_response):
        return find_client_response

    @pytest.fixture
    async def returned(self, balance_client, merchant_uid):
        return await balance_client.find_client(merchant_uid)

    def test_request_call(self, balance_client, merchant_uid, returned):
        assert balance_client.call_kwargs == {
            'method_name': 'FindClient',
            'data': ({'PassportID': str(merchant_uid)},),
        }

    def test_returns_client(self, client_entity, returned):
        assert returned == client_entity

    class TestNoClient:
        @pytest.fixture
        def client_id(self):
            return None

        def test_returns_none(self, returned):
            assert returned is None
