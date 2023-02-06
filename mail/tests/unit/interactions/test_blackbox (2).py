import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.interactions.blackbox.exceptions import BlackboxUserNotFoundError


class TestGetSUID:
    @pytest.fixture
    def uid(self):
        return 131313

    @pytest.fixture
    def blackbox_client(self, clients, mock_blackbox, response):
        mock_blackbox('/blackbox', response)
        return clients.blackbox

    @pytest.fixture
    def request_coro(self, blackbox_client, uid):
        return blackbox_client.get_suid(uid)

    class TestSuccess:
        @pytest.fixture
        def response(self, suid):
            return {'users': [{'dbfields': {'subscription.suid.2': suid}}]}

        @pytest.fixture(autouse=True)
        async def returned(self, request_coro):
            return await request_coro

        def test_returned(self, returned, suid):
            assert_that(returned, equal_to(suid))

        def test_params(self, get_last_blackbox_request, uid):
            assert_that(
                get_last_blackbox_request().query,
                equal_to({
                    'method': 'userinfo',
                    'userip': '127.0.0.1',
                    'uid': str(uid),
                    'dbfields': 'subscription.suid.2',
                    'format': 'json',
                })
            )

    class TestNotFound:
        @pytest.fixture
        def response(self):
            return {'users': [{}]}

        @pytest.mark.asyncio
        async def test_raises_not_found(self, request_coro):
            with pytest.raises(BlackboxUserNotFoundError):
                await request_coro
