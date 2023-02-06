import pytest

from aiohttp.test_utils import make_mocked_request
from aiohttp.web_exceptions import HTTPUnauthorized
from collections import defaultdict
from tvm2.ticket import UserTicket
from typing import Any
from unittest.mock import AsyncMock


from crm.agency_cabinet.common.server.common.tvm import TvmClient
from crm.agency_cabinet.common.server.web.middlewares import TvmUserMiddleware


@pytest.fixture
def middleware(tvm_client: TvmClient):
    return TvmUserMiddleware(tvm_client=tvm_client, development_mode=False)


@pytest.fixture
def middleware_dev(tvm_client: TvmClient):
    return TvmUserMiddleware(tvm_client=tvm_client, development_mode=True)


async def test_does_not_set_user_ticket_header(middleware, handler):
    request = make_mocked_request('GET', '/')

    with pytest.raises(HTTPUnauthorized, match='Header X-Ya-User-Ticket not found'):
        await middleware(request, handler)


async def test_invalid_ticket(middleware, handler, tvm_client: Any):
    ticket = 'some_user_ticket'
    request = make_mocked_request('GET', '/', headers={'X-Ya-User-Ticket': ticket})
    tvm_client.parse_user_ticket.return_value = None

    with pytest.raises(HTTPUnauthorized, match='X-Ya-User-Ticket is invalid'):
        await middleware(request, handler)

    tvm_client.parse_user_ticket.assert_awaited_with(ticket)


async def test_ok(middleware, handler: AsyncMock, tvm_client: Any):
    ticket = 'some_user_ticket'
    request = make_mocked_request('GET', '/', headers={'X-Ya-User-Ticket': ticket})
    tvm_client.parse_user_ticket.return_value = UserTicket(defaultdict(int, default_uid=42))

    await middleware(request, handler)

    handler.assert_awaited_with(request)
    assert request['yandex_uid'] == 42


# async def test_no_check_dev_header_in_prod(middleware, handler: AsyncMock):
#     request = make_mocked_request('GET', '/', headers={'No-Check-User-Ticket': '42'})
#
#     with pytest.raises(HTTPUnauthorized, match='Header X-Ya-User-Ticket not found'):
#         await middleware(request, handler)


async def test_dev_header_in_prod(middleware, handler: AsyncMock):
    request = make_mocked_request('GET', '/', headers={'No-Check-User-Ticket': '42'})

    await middleware(request, handler)

    handler.assert_awaited_with(request)
    assert request['yandex_uid'] == 42


async def test_development_mode_without_dev_header(middleware_dev, handler: AsyncMock):
    request = make_mocked_request('GET', '/')

    with pytest.raises(HTTPUnauthorized, match='Header X-Ya-User-Ticket not found'):
        await middleware_dev(request, handler)


async def test_development_mode_with_dev_header(middleware_dev, handler: AsyncMock):
    request = make_mocked_request('GET', '/', headers={'No-Check-User-Ticket': '42'})

    await middleware_dev(request, handler)

    handler.assert_awaited_with(request)
    assert request['yandex_uid'] == 42
