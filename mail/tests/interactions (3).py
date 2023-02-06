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
from mail.ipa.ipa.interactions import InteractionClients
from mail.ipa.ipa.tests.utils import get_host


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
                headers = {
                    k: v
                    for k, v in r.headers.items()
                    # otherwise "unexpected content-length header" happens
                    if k.lower() in ('content-type', 'content-disposition')
                }
                text = await r.text()
                response = self.Response(text=text, status=r.status, headers=headers)
                return response


@pytest.fixture
async def mock_server(event_loop):
    async with ResponsesMockServer(loop=event_loop) as server:
        yield server


@pytest.fixture
def mock_response_json():
    def _inner(body: dict, status=200):
        return web.Response(status=status, body=ujson.dumps(body), headers={'Content-Type': 'application/json'})

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
def last_client_request(client_requests):
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
                    if request.content_type == 'application/json':
                        await request.json()
                    else:
                        await request.post()
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


def create_client_mocker(url_settings_key):
    @pytest.fixture
    def mock_custom_client(ipa_settings, mock_client):
        host = get_host(getattr(ipa_settings, url_settings_key))
        return lambda *args, **kwargs: mock_client(host, *args, **kwargs)

    @pytest.fixture
    def last_request(ipa_settings, last_client_request):
        host = get_host(getattr(ipa_settings, url_settings_key))
        return lambda: last_client_request(host)

    @pytest.fixture
    def requests(ipa_settings, client_requests):
        host = get_host(getattr(ipa_settings, url_settings_key))
        return lambda: client_requests[host]

    return mock_custom_client, last_request, requests


mock_mds_write, get_last_mds_write_request, get_mds_write_requests = create_client_mocker('MDS_WRITE_URL')
mock_mds_read, get_last_mds_read_request, get_mds_read_requests = create_client_mocker('MDS_READ_URL')

mock_directory, get_last_directory_request, get_directory_requests = create_client_mocker('DIRECTORY_API_URL')
mock_yarm, get_last_yarm_request, get_yarm_requests = create_client_mocker('YARM_API_URL')
mock_blackbox, get_last_blackbox_request, get_blackbox_requests = create_client_mocker('BLACKBOX_API_URL')
