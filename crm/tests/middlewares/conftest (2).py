import pytest
from aiohttp import web

from crm.agency_cabinet.gateway.server.src.exceptions import ProcedureException
from crm.agency_cabinet.gateway.server.src.structs import ErrorMessage


@pytest.fixture
async def handler_error_message():
    def handler(request: web.Request) -> web.Response:
        raise ProcedureException(ErrorMessage(
            text='Page {was} not found', params={
                'was': 'is'}), http_code=404, error_code='NOT_FOUND')

    return handler


@pytest.fixture
async def handler_multiple_error_message():
    def handler(request: web.Request) -> web.Response:
        raise ProcedureException(
            ErrorMessage(text='Page {was} not found', params={'was': 'is'}),
            ErrorMessage(text='Unable to load page {page_desc}, because it does not exist',
                         params={'page_desc': '\'About company\''}), http_code=404, error_code='NOT_FOUND')

    return handler


@pytest.fixture
async def handler_str():
    def handler(request: web.Request) -> web.Response:
        raise ProcedureException('Page is not found', http_code=404, error_code='NOT_FOUND')

    return handler


@pytest.fixture
async def handler_multiple_str():
    def handler(request: web.Request) -> web.Response:
        raise ProcedureException('Page is not found', 'Unable to load page', http_code=404, error_code='NOT_FOUND')

    return handler


@pytest.fixture
async def handler_mixed_valid():
    def handler(request: web.Request) -> web.Response:
        raise ProcedureException(ErrorMessage(text='Page {was} not found', params={'was': 'is'}),
                                 'Unable to load page', http_code=404, error_code='NOT_FOUND')

    return handler


@pytest.fixture
async def handler_mixed_invalid():
    def handler(request: web.Request) -> web.Response:
        raise ProcedureException(
            ErrorMessage(
                text='Page {was} not found', params={
                    'was': 'is'}), 0, http_code=404, error_code='NOT_FOUND')

    return handler
