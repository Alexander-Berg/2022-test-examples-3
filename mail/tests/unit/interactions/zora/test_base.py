import pytest

from mail.payments.payments.interactions.zora import AbstractZoraClient
from mail.payments.payments.tests.utils import dummy_coro


class TestBaseZora:
    @pytest.fixture
    def args(self):
        return '', '', ''

    @pytest.fixture(autouse=True)
    def make_request_mock(self, mocker, response_mock):
        mock = mocker.patch(
            'mail.payments.payments.interactions.zora.AbstractInteractionClient._make_request',
            mocker.Mock(return_value=dummy_coro(response_mock)),
        )
        yield mock
        mock.return_value.close()

    @pytest.fixture
    def zora_url(self):
        return 'test-base-zora-zora-url'

    @pytest.fixture(autouse=True)
    def settings_override(self, payments_settings, zora_url):
        payments_settings.ZORA_URL = zora_url

    @pytest.fixture
    def zora_client(self, test_logger):
        return AbstractZoraClient(test_logger, 'test-base-zora-request-id')

    @pytest.mark.asyncio
    async def test_sets_proxy_kwarg(self, make_request_mock, zora_url, zora_client, args):
        await zora_client._make_request(*args, proxy='not-zora-url')
        assert make_request_mock.call_args[1]['proxy'] == zora_url

    @pytest.mark.parametrize('zora_verify_cert,header_value', [
        (False, 'true'),
        (True, 'false'),
    ])
    @pytest.mark.asyncio
    async def test_sets_ssl_cert_policy(self, make_request_mock, zora_client, zora_verify_cert, header_value, args):
        zora_client.ZORA_VERIFY_CERT = zora_verify_cert
        await zora_client._make_request(*args)
        assert make_request_mock.call_args[1]['headers']['X-Ya-Ignore-Certs'] == header_value
