import pytest

from aiohttp.test_utils import make_mocked_request
from aiohttp.web_exceptions import HTTPUnauthorized, HTTPForbidden
from collections import defaultdict
from tvm2.ticket import ServiceTicket
from typing import Any
from unittest.mock import AsyncMock

from crm.agency_cabinet.common.server.common.tvm import TvmClient
from crm.agency_cabinet.common.server.web.middlewares import TvmServiceMiddleware


@pytest.fixture
def middleware(tvm_client: TvmClient):
    return TvmServiceMiddleware(tvm_client=tvm_client, allowed_clients=[123], development_mode=False)


@pytest.fixture
def middleware_dev(tvm_client: TvmClient):
    return TvmServiceMiddleware(tvm_client=tvm_client, allowed_clients=[123], development_mode=True)


async def test_does_not_set_service_ticket_header(middleware, handler):
    request = make_mocked_request('GET', '/')

    with pytest.raises(HTTPUnauthorized, match='Header X-Ya-Service-Ticket not found'):
        await middleware(request, handler)


async def test_invalid_ticket(middleware, handler, tvm_client: Any):
    ticket = 'some_ticket'
    request = make_mocked_request('GET', '/', headers={'X-Ya-Service-Ticket': ticket})
    tvm_client.parse_service_ticket.return_value = None

    with pytest.raises(HTTPUnauthorized, match='Empty parsed TVM service ticket'):
        await middleware(request, handler)

    tvm_client.parse_service_ticket.assert_awaited_with(ticket)


async def test_unknown_client_id(middleware, handler, tvm_client: Any):
    ticket = 'some_ticket'
    request = make_mocked_request('GET', '/', headers={'X-Ya-Service-Ticket': ticket})
    tvm_client.parse_service_ticket.return_value = ServiceTicket(defaultdict(int, src=456))

    with pytest.raises(HTTPForbidden, match='Unknown source: 456'):
        await middleware(request, handler)

    tvm_client.parse_service_ticket.assert_awaited_with(ticket)


async def test_ok(middleware, handler: AsyncMock, tvm_client: Any):
    ticket = 'some_ticket'
    request = make_mocked_request('GET', '/', headers={'X-Ya-Service-Ticket': ticket})
    tvm_client.parse_service_ticket.return_value = ServiceTicket(defaultdict(int, src=123))

    await middleware(request, handler)

    handler.assert_awaited_with(request)


async def test_no_check_dev_header_in_prod(middleware, handler: AsyncMock):
    request = make_mocked_request('GET', '/', headers={'No-Check-Service-Ticket': 'any'})

    with pytest.raises(HTTPUnauthorized, match='Header X-Ya-Service-Ticket not found'):
        await middleware(request, handler)


async def test_development_mode_without_dev_header(middleware_dev, handler: AsyncMock):
    request = make_mocked_request('GET', '/')

    with pytest.raises(HTTPUnauthorized, match='Header X-Ya-Service-Ticket not found'):
        await middleware_dev(request, handler)


async def test_development_mode_with_dev_header(middleware_dev, handler: AsyncMock):
    request = make_mocked_request('GET', '/', headers={'No-Check-Service-Ticket': 'any'})

    await middleware_dev(request, handler)

    handler.assert_awaited_with(request)
