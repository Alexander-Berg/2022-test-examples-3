from json import loads

import pytest
from aiohttp.test_utils import make_mocked_request

from crm.agency_cabinet.gateway.server.src.middlewares.error import ErrorMiddleware


@pytest.fixture
def middleware():
    return ErrorMiddleware()


async def test_single_error_message(middleware, handler_error_message):
    expected_response = '''{
    "error": {
        "http_code": 404,
        "error_code": "NOT_FOUND",
        "messages": [
            {
                "text": "Page {was} not found",
                "params": {
                    "was": "is"
                }
            }
        ]
    }
}'''

    request = make_mocked_request('GET', '/ping_with_auth')
    response = await middleware(request, handler_error_message)

    assert loads(expected_response) == loads(response.body)


async def test_multiple_error_message(middleware, handler_multiple_error_message):
    expected_response = '''{
    "error": {
        "http_code": 404,
        "error_code": "NOT_FOUND",
        "messages": [
            {
                "text": "Page {was} not found",
                "params": {
                    "was": "is"
                }
            },
            {
                "text": "Unable to load page {page_desc}, because it does not exist",
                "params": {
                    "page_desc": "'About company'"
                }
            }
        ]
    }
}'''

    request = make_mocked_request('GET', '/ping_with_auth')
    response = await middleware(request, handler_multiple_error_message)

    assert loads(expected_response) == loads(response.body)


async def test_single_str(middleware, handler_str):
    expected_response = '''{
    "error": {
        "http_code": 404,
        "error_code": "NOT_FOUND",
        "messages": [
            {
                "text": "Page is not found",
                "params": {}
            }
        ]
    }
}'''

    request = make_mocked_request('GET', '/ping_with_auth')
    response = await middleware(request, handler_str)

    assert loads(expected_response) == loads(response.body)


async def test_multiple_str(middleware, handler_multiple_str):
    expected_response = '''{
    "error": {
        "http_code": 404,
        "error_code": "NOT_FOUND",
        "messages": [
            {
                "text": "Page is not found",
                "params": {}
            },
            {
                "text": "Unable to load page",
                "params": {}
            }
        ]
    }
}'''

    request = make_mocked_request('GET', '/ping_with_auth')
    response = await middleware(request, handler_multiple_str)

    assert loads(expected_response) == loads(response.body)


async def test_mixed_valid(middleware, handler_mixed_valid):
    expected_response = '''{
    "error": {
        "http_code": 404,
        "error_code": "NOT_FOUND",
        "messages": [
            {
                "text": "Page {was} not found",
                "params": {
                    "was": "is"
                }
            },
            {
                "text": "Unable to load page",
                "params": {}
            }
        ]
    }
}'''

    request = make_mocked_request('GET', '/ping_with_auth')
    response = await middleware(request, handler_mixed_valid)

    assert loads(expected_response) == loads(response.body)


async def test_mixed_invalid(middleware, handler_mixed_invalid):
    expected_response = '''{
    "error": "Expected 'str' or 'ErrorMessage', found: <class 'int'>"
}'''

    request = make_mocked_request('GET', '/ping_with_auth')
    response = await middleware(request, handler_mixed_invalid)

    assert loads(expected_response) == loads(response.body)
