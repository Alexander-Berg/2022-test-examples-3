import pytest

from mail.payments.payments.api.exceptions import APIException
from mail.payments.payments.api_sdk.middlewares import middleware_request_checker
from mail.payments.payments.tests.utils import dummy_async_function


@pytest.mark.usefixtures('loop')
class TestRequestChecker:
    @pytest.fixture
    def route_name(self, rands):
        return rands()

    @pytest.fixture(params=(True, False))
    def route_is_internal(self, request):
        return request.param

    @pytest.fixture(params=(True, False))
    def headers(self, request):
        return {'X-External-Request': 'true'} if request.param else {}

    @pytest.fixture(autouse=True)
    def setup(self, payments_settings, route_name, route_is_internal):
        payments_settings.SDK_INTERNAL_PATHS = (route_name,) if route_is_internal else ()

    @pytest.fixture
    def web_request(self, noop, mocker, headers, route_name):
        web_request = mocker.Mock()
        web_request.match_info.route.name = route_name
        web_request.headers = headers
        web_request.__setitem__ = noop
        return web_request

    @pytest.fixture
    def handler(self, route_name):
        return dummy_async_function()

    @pytest.fixture
    def returned_func(self, web_request, handler):
        async def _inner():
            return await middleware_request_checker(web_request, handler)

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return returned_func()

    @pytest.mark.asyncio
    async def test_middleware(self, noop_manager, route_is_internal, headers, returned_func):
        manager = pytest.raises if headers and route_is_internal else noop_manager
        with manager(APIException):
            await returned_func()
