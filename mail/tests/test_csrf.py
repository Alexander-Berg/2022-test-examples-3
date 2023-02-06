import pytest
from aiohttp import hdrs, web_urldispatcher

from hamcrest import assert_that, equal_to

from sendr_auth.csrf import CsrfChecker, CsrfSettings
from sendr_auth.entities import User
from sendr_auth.exceptions import InvalidCSRFTokenException
from sendr_auth.middlewares import create_csrf_middleware, skip_csrf_check

UID = 555
YANDEXUID = 'the-yandexuid'
HMAC_SECRET = 'unit-test-secret'
TOKEN = 'b2edd13faeb0d818fcd8edd1637c7269e333bd4a:1608269559'
TOKEN_TIMESTAMP = 1608269559
TOKEN_WITHOUT_UID = '4d49f3b56f1b5c04e0e24f7c305ac6ce2d267b85:1608269559'


@pytest.fixture
def user():
    return User(UID)


@pytest.fixture
def settings():
    return CsrfSettings(
        keys=[HMAC_SECRET],
        token_lifetime=10**9,
    )


@pytest.fixture
def params(user, mocked_logger, settings):
    return dict(
        user=user,
        logger=mocked_logger,
        settings=settings,
        yandexuid=YANDEXUID,
        csrf_token=TOKEN,
    )


def test_generate_token(user):
    assert_that(
        CsrfChecker.generate_token(
            key=HMAC_SECRET, user=user, yandexuid=YANDEXUID, timestamp=TOKEN_TIMESTAMP
        ),
        equal_to(TOKEN),
    )


class TestIsTokenValid:
    def test_success(self, params):
        checker = CsrfChecker(**params)

        assert_that(
            checker._is_token_valid(HMAC_SECRET, TOKEN_TIMESTAMP),
            equal_to(True)
        )

    def test_success_without_uid(self, params, settings):
        params['csrf_token'] = TOKEN_WITHOUT_UID
        settings.check_token_without_uid = True
        checker = CsrfChecker(**params)

        assert_that(
            checker._is_token_valid(HMAC_SECRET, TOKEN_TIMESTAMP),
            equal_to(True)
        )

    def test_failure_without_uid(self, params, settings):
        params['csrf_token'] = TOKEN_WITHOUT_UID
        checker = CsrfChecker(**params)

        assert_that(
            checker._is_token_valid(HMAC_SECRET, TOKEN_TIMESTAMP),
            equal_to(False)
        )


class TestHandle:
    @pytest.fixture(autouse=True)
    def mock_is_token_valid(self, mocker):
        return mocker.patch.object(CsrfChecker, '_is_token_valid', mocker.Mock(return_value=True))

    @pytest.mark.asyncio
    async def test_success(self, params):
        checker = CsrfChecker(**params)

        checker.check()

    @pytest.mark.asyncio
    async def test_no_yandexcookie(self, params):
        params['yandexuid'] = None
        checker = CsrfChecker(**params)

        with pytest.raises(InvalidCSRFTokenException):
            checker.check()

    @pytest.mark.asyncio
    async def test_no_token(self, params):
        params['csrf_token'] = None
        checker = CsrfChecker(**params)

        with pytest.raises(InvalidCSRFTokenException):
            checker.check()

    @pytest.mark.asyncio
    async def test_token_expired(self, params, settings):
        settings.token_lifetime = 0
        checker = CsrfChecker(**params)

        with pytest.raises(InvalidCSRFTokenException):
            checker.check()

    @pytest.mark.asyncio
    async def test_malformed_timestamp(self, params):
        params['csrf_token'] = TOKEN.split(':')[0] + ':malformed'
        checker = CsrfChecker(**params)

        with pytest.raises(InvalidCSRFTokenException):
            checker.check()

    @pytest.mark.asyncio
    async def test_token_invalid(self, mocker, params):
        checker = CsrfChecker(**params)
        mocker.patch.object(CsrfChecker, '_is_token_valid', mocker.Mock(return_value=False))

        with pytest.raises(InvalidCSRFTokenException):
            checker.check()


class TestMiddleware:
    @pytest.fixture
    def request_obj(self, user, settings, mocked_logger):
        class Request(dict):
            pass

        request = Request()
        request['logger'] = mocked_logger
        request['user'] = user
        request.cookies = {'yandexuid': YANDEXUID}
        request.headers = {settings.header_name: TOKEN}
        request.method = hdrs.METH_POST
        request.match_info = None
        return request

    @pytest.fixture
    def middleware(self, settings):
        return create_csrf_middleware(settings)

    @pytest.mark.asyncio
    async def test_success(self, handler, middleware, request_obj):
        await middleware(request_obj, handler)

        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_fail(self, handler, middleware, request_obj):
        request_obj.cookies['yandexuid'] = 'incorrect'

        with pytest.raises(InvalidCSRFTokenException):
            await middleware(request_obj, handler)

    @pytest.mark.asyncio
    async def test_safe_method(self, mocker, handler, middleware, request_obj):
        request_obj.method = hdrs.METH_GET
        mock = mocker.patch.object(CsrfChecker, 'check', mocker.Mock())

        await middleware(request_obj, handler)

        mock.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_no_user(self, handler, mocker, middleware, request_obj):
        del request_obj['user']
        mock = mocker.patch.object(CsrfChecker, 'check', mocker.Mock())

        await middleware(request_obj, handler)

        mock.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_no_cookies(self, handler, mocker, middleware, request_obj):
        request_obj.cookies = {}
        mock = mocker.patch.object(CsrfChecker, 'check', mocker.Mock())

        await middleware(request_obj, handler)

        mock.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_skip(self, handler, mocker, middleware, request_obj):
        handler = skip_csrf_check(handler)
        mock = mocker.patch.object(CsrfChecker, 'check', mocker.Mock())

        await middleware(request_obj, handler)

        mock.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_not_found(self, handler, mocker, middleware, request_obj):
        request_obj.match_info = web_urldispatcher.MatchInfoError(Exception)
        mock = mocker.patch.object(CsrfChecker, 'check', mocker.Mock())

        await middleware(request_obj, handler)

        mock.assert_not_called()
        handler.assert_awaited_once_with(request_obj)
