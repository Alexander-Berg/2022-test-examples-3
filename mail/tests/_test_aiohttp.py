import pytest
from aiohttp import web

from sendr_qlog.http.aiohttp import get_middleware_logging_adapter, signal_request_id_header


class HandlerTestContract:
    def test_request_contains_request_id(self, aiohttp_request):
        assert aiohttp_request.get('request-id')

    def test_request_has_logger(self, aiohttp_request):
        assert aiohttp_request.get('logger')

    def test_logger_contains_request_id(self, aiohttp_request):
        logger_context = aiohttp_request['logger'].get_context()
        assert logger_context.get('request-id') == aiohttp_request.get('request-id')


class TestMiddleware:
    @pytest.fixture
    def test_handler(self, mocker):
        return mocker.Mock(return_value=web.Response())

    @pytest.fixture
    def aiohttp_request(self, response, test_handler):
        return test_handler.call_args[0][0]

    @pytest.fixture
    def app(self, test_handler):
        async def async_test_handler(request):
            return test_handler(request)

        app = web.Application(middlewares=(get_middleware_logging_adapter(),))
        app.router.add_get('/', async_test_handler)
        app.on_response_prepare.append(signal_request_id_header)
        return app

    @pytest.fixture
    async def client(self, loop, app, aiohttp_client):
        return await aiohttp_client(app)

    @pytest.mark.parametrize('header_name', ('X-Request-Id', 'X-Req-Id'))
    class TestWhenXRequestIdPassed(HandlerTestContract):
        @pytest.fixture
        def request_id(self):
            return 'test_req_id'

        @pytest.fixture
        async def response(self, loop, client, header_name, request_id):
            return await client.get('/', headers={header_name: request_id})

        def test_response_request_id(self, response, request_id):
            assert response.headers['X-Request-Id'] == request_id

    class TestWhenNoRequestIdPassed(HandlerTestContract):
        @pytest.fixture
        async def response(self, loop, client):
            return await client.get('/')

        def test_generates_request_id(self, response):
            assert response.headers['X-Request-Id']

        def test_generated_request_id_same_as_in_request_object(self, response, aiohttp_request):
            assert response.headers['X-Request-Id'] == aiohttp_request.get('request-id')
