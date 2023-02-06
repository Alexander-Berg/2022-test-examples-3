from typing import Any
from unittest.mock import AsyncMock
from aiohttp.test_utils import make_mocked_request


async def test_tvm2_request_with_ticket(middleware_tvm_on, handler: AsyncMock, tvm_client: Any):
    ticket = 'ticket'
    tvm_client.parse_service_ticket.return_value = 'ticket'
    request = make_mocked_request('GET', '/', headers={'X-Ya-Service-Ticket': ticket})
    await middleware_tvm_on(request, handler)
    handler.assert_awaited_with(request)


async def test_tvm2_request_with_bad_ticket(middleware_tvm_on, handler: AsyncMock, tvm_client: Any):
    ticket = 'bad ticket'
    tvm_client.parse_service_ticket.return_value = None
    request = make_mocked_request('GET', '/', headers={'X-Ya-Service-Ticket': ticket})
    response = await middleware_tvm_on(request, handler)
    assert response.status == 403


async def test_tvm2_request_no_headers(middleware_tvm_on, handler: AsyncMock, tvm_client: Any):
    request = make_mocked_request('GET', '/', headers={})
    response = await middleware_tvm_on(request, handler)
    assert response.status == 401


async def test_tvm2_request_get_ping_tvm_on(middleware_tvm_on, handler: AsyncMock, tvm_client: Any):
    tvm_client.parse_service_ticket.return_value = None
    request = make_mocked_request('GET', '/ping', headers={})
    request.match_info.route.name = 'ping'
    await middleware_tvm_on(request, handler)
    handler.assert_awaited_with(request)


async def test_tvm2_request_tvm_off(middleware_tvm_off, handler: AsyncMock, tvm_client: Any):
    request = make_mocked_request('GET', '/', headers={})
    await middleware_tvm_off(request, handler)
    handler.assert_awaited_with(request)


async def test_tvm2_request_get_ping_tvm_off(middleware_tvm_off, handler: AsyncMock, tvm_client: Any):
    tvm_client.parse_service_ticket.return_value = None
    request = make_mocked_request('GET', '/ping', headers={})
    request.match_info.route.name = 'ping'
    await middleware_tvm_off(request, handler)
    handler.assert_awaited_with(request)
