from base64 import b64encode

import jwt
import pytest
import ujson

from sendr_utils import json_value

from mail.payments.payments.tests.utils import dummy_coro


class TestCallback:
    @pytest.fixture
    def url(self, rands):
        return rands()

    @pytest.fixture
    def secret(self, rands):
        return rands()

    @pytest.fixture
    def message(self, rands, randn):
        return dict(((rands(), rands()) for _ in range(randn(min=2, max=10))))

    @pytest.fixture
    def signer(self):
        def _signer(inp: bytes) -> bytes:
            return bytes(reversed(inp))

        return _signer

    @pytest.mark.asyncio
    async def test_private_post_signed_message(self, url, message, secret, callback_client):
        await callback_client._post_signed_message(url, message=message, sign=secret)

        assert all([
            callback_client.call_args == ('post_signed_message', 'POST', url),
            callback_client.call_kwargs == {
                'json': {
                    'message': message,
                    'sign': secret,
                }
            },
        ])

    @pytest.mark.parametrize('message', ['Alice', ''])
    def test_sign_method(self, callback_client, message, signer):
        expected = b64encode(signer(message.encode('utf-8'))).decode('utf-8')
        assert expected == callback_client.sign_message(message, signer)

    @pytest.mark.asyncio
    async def test_post_signed_message(self, url, message, callback_client, mocker, signer):
        mock = mocker.patch.object(callback_client, '_post_signed_message', mocker.Mock(return_value=dummy_coro()))
        message_dump = ujson.dumps(json_value(message))

        await callback_client.post_signed_message(url=url, message=message, signer=signer)

        assert mock.called_once_with(
            url=url,
            message=message_dump,
            sign=callback_client.sign_message(message_dump, signer)
        )

    @pytest.mark.asyncio
    async def test_post_jwt_message(self, url, message, secret, callback_client, mocker, signer):
        mock = mocker.patch.object(callback_client, '_post_message', mocker.Mock(return_value=dummy_coro()))

        await callback_client.post_jwt_message(url=url, message=message, secret=secret)

        assert mock.called_once_with(
            url=url,
            message=jwt.encode(message, secret, algorithm='HS256')
        )
