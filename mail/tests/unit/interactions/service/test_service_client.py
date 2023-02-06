import re

import pytest

from hamcrest import assert_that, has_entries, not_none


@pytest.mark.asyncio
async def test_callback_method_involves_post_call(aioresponses_mocker, service_client, request_id):
    mock_service = aioresponses_mocker.post(
        re.compile('^http://service.domain$'),
        payload={}
    )

    url = 'http://service.domain'
    json = {'a': 'a', 'b': 'b'}
    await service_client.callback_service(url=url, json=json)
    assert_that(
        mock_service.call_args[1],
        has_entries({
            'headers': has_entries({
                'X-Ya-Service-Ticket': not_none(),
                'X-Request-Id': request_id,
            }),
            'json': json,
        })
    )


@pytest.mark.parametrize('tvm_id', (None,))
@pytest.mark.asyncio
async def test_callback_no_tvm(aioresponses_mocker, service_client, request_id):
    mock_service = aioresponses_mocker.post(
        re.compile('^http://service.domain$'),
        payload={}
    )

    url = 'http://service.domain'
    json = {'a': 'a', 'b': 'b'}
    await service_client.callback_service(url=url, json=json)
    assert_that(
        mock_service.call_args[1],
        has_entries({
            'headers': {
                'X-Request-Id': request_id,
            },
            'json': json,
        })
    )


@pytest.mark.asyncio
async def test_callback_service_json(mocker, service_client, aioresponses_mocker):
    aioresponses_mocker.post(
        re.compile('^http://service.domain$'),
        body='not-a-json'
    )

    url = 'http://service.domain'
    json = {'a': 'a', 'b': 'b'}
    await service_client.callback_service(url=url, json=json)  # should not raise
