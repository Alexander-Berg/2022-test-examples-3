import pytest
import ujson

from mail.payments.payments.interactions.developer.exceptions import DeveloperKeyAccessDeny, DeveloperServiceNotFound


class TestCheckKey:
    @pytest.fixture
    def key(self):
        return 'test-check-key-key'

    @pytest.fixture
    def user_ip(self):
        return 'test-check-key-user-ip'

    @pytest.fixture
    def response_data(self, merchant):
        return ujson.dumps({
            'key_info': {
                'user': {
                    'uid': merchant.uid,
                }
            },
        })

    @pytest.fixture
    def response_func(self, developer_client, key, user_ip):
        async def _inner():
            return await developer_client.check_key(key, user_ip)

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    def test_returns_uid(self, merchant, response):
        assert response == merchant.uid

    @pytest.mark.asyncio
    async def test_overwrite_uid(self, payments_settings, randn, response_func):
        payments_settings.DEVELOPER_OVERWRITE_UID = randn()
        uid = await response_func()
        assert uid == payments_settings.DEVELOPER_OVERWRITE_UID

    class TestNotFoundErrors:
        @pytest.fixture(params=[
            ('Key not found', DeveloperKeyAccessDeny),
            ('Service not found', DeveloperServiceNotFound),
        ])
        def message_exception(self, request):
            return request.param

        @pytest.fixture
        def message(self, message_exception):
            return message_exception[0]

        @pytest.fixture
        def exception(self, message_exception):
            return message_exception[1]

        @pytest.fixture
        def response_status(self):
            return 404

        @pytest.fixture
        def response_data(self, message):
            return ujson.dumps({'error': message})

        @pytest.mark.asyncio
        async def test_not_found_errors__raises_error(self, exception, response_func):
            with pytest.raises(exception):
                await response_func()

    class TestForbiddenError:
        @pytest.fixture
        def response_status(self):
            return 403

        @pytest.mark.asyncio
        async def test_forbidden__raises_error(self, response_func):
            with pytest.raises(DeveloperKeyAccessDeny):
                await response_func()
