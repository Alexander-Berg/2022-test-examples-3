import re
from unittest import mock

import pytest
from aiohttp import TCPConnector

from sendr_interactions.clients.zora import AbstractZoraClient
from sendr_interactions.clients.zora.exceptions import ZoraRemoteHostCertificateError
from sendr_interactions.exceptions import InteractionResponseError

from hamcrest import assert_that, equal_to, has_entries

FAKE_URL = 'http://example.fake'
NUM_RETRIES = 2


tvm_ticket_getter_mock = mock.AsyncMock()
tvm_ticket_getter_mock.get_service_ticket_headers = mock.AsyncMock(return_value={'X-Tvm_ticket': '123'})


class BaseZoraClient(AbstractZoraClient):
    SERVICE = 'zora'
    DEBUG = False

    ZORA_VERIFY_CERT = True
    ZORA_FOLLOW_REDIRECTS = True
    ZORA_CLIENT_NAME = 'zora'
    ZORA_TVM_ID = 123
    ZORA_URL = 'https://test.test'
    ZORA_ERROR_CODE_HEADER = 'X-Yandex-Gozora-Error-Code'
    ZORA_ERROR_DESC_HEADER = 'X-Yandex-Gozora-Error-Description'

    tvm_ticket_getter = tvm_ticket_getter_mock

    CONNECTOR = TCPConnector()


@pytest.fixture
def fake_gozora_response():
    return {'fake_gozora_response': True}


@pytest.fixture
def gozora_responses_mocker(
    aioresponses_mocker, fake_gozora_response
):
    return aioresponses_mocker.get(
        re.compile(f'^{BaseZoraClient.ZORA_URL}.*'),
        status=200,
        payload=fake_gozora_response,
    )


@pytest.fixture
async def zora_client(create_interaction_client):
    client = create_interaction_client(BaseZoraClient)
    client.REQUEST_RETRY_TIMEOUTS = (0.01,) * NUM_RETRIES
    yield client
    await client.close()


@pytest.mark.asyncio
async def test_successful_request(
    zora_client, gozora_responses_mocker, fake_gozora_response, request_id
):
    response = await zora_client.get('get_fake', FAKE_URL)
    assert_that(response, equal_to(fake_gozora_response))

    gozora_responses_mocker.assert_called_once()
    _, call_kwargs = gozora_responses_mocker.call_args_list[0]
    assert_that(
        call_kwargs,
        has_entries(
            headers=has_entries(
                {
                    'X-Ya-Ignore-Certs': 'false',
                    'X-Ya-Follow-Redirects': 'true',
                    'X-Ya-Req-Id': request_id,
                    'X-Ya-Dest-Url': FAKE_URL,
                    'X-Ya-Client-Id': BaseZoraClient.ZORA_CLIENT_NAME,
                }
            ),
            verify_ssl=False,
        )
    )


@pytest.mark.asyncio
async def test_remote_host_certificate_error_not_retried(
    aioresponses_mocker, zora_client
):
    zora_error_message = 'zora error message'
    mock_http = aioresponses_mocker.get(
        re.compile(f'^{BaseZoraClient.ZORA_URL}.*'),
        status=599,
        headers={
            BaseZoraClient.ZORA_ERROR_CODE_HEADER: '1000',
            BaseZoraClient.ZORA_ERROR_DESC_HEADER: zora_error_message,
        },
        repeat=True,
    )

    with pytest.raises(ZoraRemoteHostCertificateError, match=zora_error_message):
        await zora_client.get('get_fake', FAKE_URL)

    mock_http.assert_called_once()


@pytest.mark.asyncio
async def test_generic_5xx_response_retried(
    aioresponses_mocker, zora_client
):
    mock_http = aioresponses_mocker.get(
        re.compile(f'^{BaseZoraClient.ZORA_URL}.*'), status=599, repeat=True
    )
    with pytest.raises(InteractionResponseError):
        await zora_client.get('get_fake', FAKE_URL)

    assert_that(mock_http.call_count, equal_to(NUM_RETRIES + 1))
