import asyncio
import re
from collections import defaultdict
from functools import partial
from typing import Optional

import pytest
import ujson
from aiohttp import ClientSession, TCPConnector, web

from aresponses import ResponsesMockServer as BaseResponsesMockServer
from aresponses.utils import ANY
from mail.beagle.beagle.interactions import InteractionClients
from mail.beagle.beagle.tests.utils import get_url_part


@pytest.fixture
async def clients(test_logger, request_id):
    clients = InteractionClients(test_logger, request_id)
    async with clients:
        yield clients


class ResponsesMockServer(BaseResponsesMockServer):
    async def passthrough(self, request):
        connector = TCPConnector()
        connector._resolve_host = partial(self._old_resolver_mock, connector)

        original_request = request.clone(scheme="https" if request.headers["AResponsesIsSSL"] else "http")
        headers = {k: v for k, v in request.headers.items() if k != "AResponsesIsSSL"}

        async with ClientSession(connector=connector) as session:
            request_method = getattr(session, request.method.lower())
            async with request_method(original_request.url, headers=headers, data=(await request.read())) as r:
                headers = {k: v for k, v in r.headers.items() if k.lower() == "content-type"}
                text = await r.text()
                response = self.Response(text=text, status=r.status, headers=headers)
                return response


@pytest.fixture
async def mock_server(event_loop):
    async with ResponsesMockServer(loop=event_loop) as server:
        yield server


@pytest.fixture
def mock_response_json():
    def _inner(body: dict):
        return web.Response(body=ujson.dumps(body), headers={'Content-Type': 'application/json'})

    return _inner


@pytest.fixture(autouse=True)
async def mock_self(mock_server):
    async def handler(request: Optional[web.BaseRequest] = None):
        mock_server.add(re.compile('127.0.0.1:[0-9]+'), ANY, ANY, handler)
        if request is not None:
            return await mock_server.passthrough(request)

    await handler()


@pytest.fixture(autouse=True)
def mock_tvm(mock_server, mock_response_json, rands, randn):
    def handler_tvm_tickets(request: Optional[web.BaseRequest] = None):
        mock_server.add(ANY, '/tvm/tickets', ANY, handler_tvm_tickets)
        if request is not None:
            return mock_response_json({rands(): {"ticket": f"3:serv:{rands()}", "tvm_id": request.query["dsts"]}})

    def handler_tvm_checksrv(request: Optional[web.BaseRequest] = None):
        mock_server.add(ANY, '/tvm/checksrv', ANY, handler_tvm_checksrv)
        if request is not None:
            return mock_response_json({
                "src": randn(),
                "dst": request.query["dst"],
                "scopes": None,
                "debug_string": rands(),
                "logging_string": rands(),
                "issuer_uid": None
            })

    handler_tvm_checksrv()
    handler_tvm_tickets()


@pytest.fixture
def client_requests():
    return defaultdict(list)


@pytest.fixture
def last_client_request(client_requests):  # TODO: remove lambda
    return lambda host: client_requests[host][-1]


@pytest.fixture
def mock_client(mock_server, mock_response_json, client_requests):
    def _inner(host: str, *args, **kwargs):
        if len(args) == 1:
            responses = args[0]
        elif len(args) == 2:
            responses = {args[0]: args[1]}
        elif 'responses' in kwargs:
            responses = kwargs['responses']
        else:
            raise Exception('Invalid mock_client params')

        for path_pattern, response in responses.items():
            if isinstance(response, (dict, list)):
                response = mock_response_json(response)

            def handler(resp):
                async def _inner(request):
                    await request.read()
                    client_requests[host].append(request)

                    if callable(resp):
                        if asyncio.iscoroutinefunction(resp):
                            return await resp(request)
                        else:
                            return resp(request)
                    else:
                        return resp

                return _inner

            mock_server.add(host, path_pattern=path_pattern, response=handler(response))

    return _inner


# Passport
@pytest.fixture
def mock_passport(beagle_settings, mock_client):
    host = get_url_part(beagle_settings.PASSPORT_API_URL, 'netloc')
    return lambda *args, **kwargs: mock_client(host, *args, **kwargs)


@pytest.fixture
def last_passport_request(beagle_settings, last_client_request):
    return lambda: last_client_request(get_url_part(beagle_settings.PASSPORT_API_URL, 'netloc'))


# Directory
@pytest.fixture
def directory_host(beagle_settings):
    return get_url_part(beagle_settings.DIRECTORY_API_URL, 'netloc')


@pytest.fixture
def mock_directory(directory_host, mock_client):
    return lambda *args, **kwargs: mock_client(directory_host, *args, **kwargs)


@pytest.fixture
def directory_requests(directory_host, client_requests):
    return client_requests[directory_host]


@pytest.fixture
def last_directory_request(directory_host, last_client_request):
    return lambda: last_client_request(directory_host)


# Blackbox
@pytest.fixture
def mock_blackbox(beagle_settings, mock_client):
    host = get_url_part(beagle_settings.BLACKBOX_API_URL, 'netloc')
    path = get_url_part(beagle_settings.BLACKBOX_API_URL, 'path')
    return lambda response: mock_client(host, path, response)


@pytest.fixture
def last_blackbox_request(beagle_settings, last_client_request):
    return lambda: last_client_request(get_url_part(beagle_settings.BLACKBOX_API_URL, 'netloc'))


@pytest.fixture
def blackbox_requests(beagle_settings, client_requests):
    return lambda: client_requests[get_url_part(beagle_settings.BLACKBOX_API_URL, 'netloc')]


# Sender
@pytest.fixture
def mock_sender(beagle_settings, mock_client):
    host = get_url_part(beagle_settings.SENDER_API_URL, 'netloc')
    return lambda *args, **kwargs: mock_client(host, *args, **kwargs)


@pytest.fixture
def last_sender_request(beagle_settings, last_client_request):
    return lambda: last_client_request(get_url_part(beagle_settings.SENDER_API_URL, 'netloc'))


# MBody
@pytest.fixture
def mock_mbody(beagle_settings, mock_client):
    host = get_url_part(beagle_settings.MBODY_API_URL, 'netloc')
    return lambda *args, **kwargs: mock_client(host, *args, **kwargs)


@pytest.fixture
def last_mbody_request(beagle_settings, last_client_request):
    return lambda: last_client_request(get_url_part(beagle_settings.MBODY_API_URL, 'netloc'))


# Hound
@pytest.fixture
def mock_hound(beagle_settings, mock_client):
    host = get_url_part(beagle_settings.HOUND_API_URL, 'netloc')
    return lambda *args, **kwargs: mock_client(host, *args, **kwargs)


@pytest.fixture
def last_hound_request(beagle_settings, last_client_request):
    return lambda: last_client_request(get_url_part(beagle_settings.HOUND_API_URL, 'netloc'))
