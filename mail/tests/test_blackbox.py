import pytest
from aiohttp import hdrs, web_urldispatcher

from sendr_interactions.clients.blackbox import AbstractBlackBoxClient, OauthResult, SessionIdResult
from sendr_interactions.clients.blackbox.exceptions import BlackBoxInvalidSessionError

from hamcrest import assert_that, equal_to, has_item, has_properties

from sendr_auth.blackbox import AuthenticationException, BlackboxAuthenticator
from sendr_auth.entities import AuthenticationMethod, User
from sendr_auth.middlewares import create_blackbox_middleware, optional_authentication, skip_authentication

FAKE_USER_TICKET = 'fake_tvm_user_ticket'
FAKE_LOGIN_ID = 'fake_login_id'
FAKE_OAUTH_CLIENT_ID = 'fake_oauth_client_id'


@pytest.fixture
def expected_uid(request):
    return getattr(request, 'param', 4029320686)


@pytest.fixture
def blackbox_client(mocked_logger):
    return AbstractBlackBoxClient(logger=mocked_logger, request_id='')


class TestSessionId:
    @pytest.fixture
    def params(self, blackbox_client, mocked_logger):
        return {
            'client': blackbox_client,
            'scopes': None,
            'authorization_header': None,
            'session_id': 'session_id',
            'user_ip': '192.0.2.1',
            'default_uid': None,
            'host': 'ya.ru',
            'logger': mocked_logger,
            'allowed_oauth_client_ids': None,
        }

    @pytest.fixture(autouse=True)
    def mock_blackbox(self, mocker, expected_uid):
        return mocker.patch.object(
            AbstractBlackBoxClient,
            'get_session_info',
            mocker.AsyncMock(return_value=SessionIdResult(expected_uid, FAKE_LOGIN_ID, FAKE_USER_TICKET, True)),
        )

    @pytest.mark.asyncio
    async def test_returns_expected_user(self, params, expected_uid, mocked_logger):
        assert_that(
            await BlackboxAuthenticator(**params).get_user(),
            has_properties(
                uid=expected_uid,
                tvm_ticket=FAKE_USER_TICKET,
                login_id=FAKE_LOGIN_ID,
                auth_method=AuthenticationMethod.SESSION,
                is_yandexoid=True,
            ),
        )

        assert_that(
            mocked_logger.context_push.call_args_list,
            has_item(
                has_properties(
                    kwargs=dict(uid=expected_uid, login_id=FAKE_LOGIN_ID, auth_source=AuthenticationMethod.SESSION)
                )
            )
        )

    @pytest.mark.asyncio
    async def test_invalid_session_id(self, mock_blackbox, params):
        mock_blackbox.side_effect = BlackBoxInvalidSessionError
        with pytest.raises(AuthenticationException):
            await BlackboxAuthenticator(**params).get_user()

    @pytest.mark.asyncio
    async def test_empty_session_id(self, mock_blackbox, params):
        params['session_id'] = ''
        with pytest.raises(AuthenticationException):
            await BlackboxAuthenticator(**params).get_user()

    @pytest.mark.asyncio
    async def test_blackbox_call(self, mock_blackbox, params):
        params['session_id'] = 'abc'
        params['default_uid'] = '12345'
        await BlackboxAuthenticator(**params).get_user()
        mock_blackbox.assert_called_once_with(
            session_id='abc',
            default_uid=12345,
            user_ip='192.0.2.1',
            host='ya.ru',
            get_user_ticket=True,
        )


class TestOAuth:
    @pytest.fixture
    def params(self, blackbox_client, mocked_logger):
        return {
            'client': blackbox_client,
            'scopes': [],
            'authorization_header': 'oauth token',
            'session_id': None,
            'user_ip': '192.0.2.1',
            'default_uid': None,
            'host': 'ya.ru',
            'logger': mocked_logger,
            'allowed_oauth_client_ids': None,
        }

    @pytest.fixture
    def blackbox_oauth_result(self, expected_uid):
        return OauthResult(
            client_id=FAKE_OAUTH_CLIENT_ID,
            uid=expected_uid,
            login_id=FAKE_LOGIN_ID,
            tvm_ticket=FAKE_USER_TICKET,
            is_yandexoid=True,
        )

    @pytest.fixture(autouse=True)
    def mock_blackbox(self, mocker, blackbox_oauth_result):
        return mocker.patch.object(
            AbstractBlackBoxClient,
            'get_oauth_token_info',
            mocker.AsyncMock(return_value=blackbox_oauth_result),
        )

    @pytest.mark.asyncio
    async def test_returns_expected_user(self, params, expected_uid, mocked_logger):
        assert_that(
            await BlackboxAuthenticator(**params).get_user(),
            has_properties(
                uid=expected_uid,
                tvm_ticket=FAKE_USER_TICKET,
                login_id=FAKE_LOGIN_ID,
                auth_method=AuthenticationMethod.OAUTH,
                is_yandexoid=True,
            ),
        )

        assert_that(
            mocked_logger.context_push.call_args_list,
            has_item(
                has_properties(
                    kwargs=dict(uid=expected_uid, login_id=FAKE_LOGIN_ID, auth_source=AuthenticationMethod.OAUTH)
                )
            )
        )

    @pytest.mark.asyncio
    async def test_returns_expected_user__if_oauth_client_allowed(self, params, expected_uid):
        params['allowed_oauth_client_ids'] = {FAKE_OAUTH_CLIENT_ID}

        assert_that(
            await BlackboxAuthenticator(**params).get_user(),
            has_properties(
                uid=expected_uid,
                tvm_ticket=FAKE_USER_TICKET,
                login_id=FAKE_LOGIN_ID,
                auth_method=AuthenticationMethod.OAUTH,
                is_yandexoid=True,
            ),
        )

    @pytest.mark.asyncio
    async def test_calls_blackbox(self, params, mock_blackbox):
        await BlackboxAuthenticator(**params).get_user()

        mock_blackbox.assert_called_once_with(
            oauth_token='token', scopes=[], user_ip='192.0.2.1', get_user_ticket=True
        )

    @pytest.mark.parametrize(
        'authorization_header, expected_message',
        (
            ('', 'MISSING_CREDENTIALS'),
            ('oauth', 'AUTHORIZATION_HEADER_MALFORMED'),
            ('bearer ToKeN', 'INCORRECT_AUTH_REALM'),
        )
    )
    @pytest.mark.asyncio
    async def test_when_oauth_token_empty__raises_exception(
        self, params, mock_blackbox, authorization_header, expected_message
    ):
        params['authorization_header'] = authorization_header
        with pytest.raises(AuthenticationException) as exc_info:
            await BlackboxAuthenticator(**params).get_user()

        assert_that(exc_info.value.message, equal_to(expected_message))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('expected_uid', (None,), indirect=True)
    async def test_when_uid_empty__raises_exception(self, params, mock_blackbox):
        with pytest.raises(AuthenticationException):
            await BlackboxAuthenticator(**params).get_user()

    @pytest.mark.asyncio
    async def test_when_token_rejected_by_blackbox__raises_exception(self, params, mock_blackbox):
        mock_blackbox.side_effect = BlackBoxInvalidSessionError

        with pytest.raises(AuthenticationException):
            await BlackboxAuthenticator(**params).get_user()

    @pytest.mark.asyncio
    async def test_when_oauth_client_not_allowed__raises_exception(self, params, mock_blackbox, mocked_logger):
        params['allowed_oauth_client_ids'] = {'another_client_id'}

        with pytest.raises(AuthenticationException):
            await BlackboxAuthenticator(**params).get_user()

        assert_that(
            mocked_logger.context_push.call_args_list,
            has_item(
                has_properties(
                    kwargs=dict(request_client_id=FAKE_OAUTH_CLIENT_ID, allowed_client_ids={'another_client_id'})
                )
            )
        )


class TestMiddleware:
    @pytest.fixture
    def mock_authenticator(self, mocker):
        return mocker.patch('sendr_auth.middlewares.BlackboxAuthenticator')

    @pytest.fixture(autouse=True)
    def mock_get_user(self, mocker, expected_uid, mock_authenticator):
        get_user_mock = mocker.AsyncMock(return_value=User(expected_uid))
        mock_authenticator.return_value.get_user = get_user_mock
        return get_user_mock

    @pytest.fixture
    def request_obj(self, expected_uid, mocked_logger):
        class Request(dict):
            pass

        request = Request()
        request['logger'] = mocked_logger
        request.path = '/'
        request.cookies = {}
        request.headers = {}
        request.query = {}
        request.remote = '127.0.0.1'
        request.method = hdrs.METH_GET
        request.match_info = None
        return request

    @pytest.fixture
    def middleware(self):
        return create_blackbox_middleware(
            AbstractBlackBoxClient,
            oauth_scopes=[],
            host='ya.ru',
            ignored_paths={'/ignored', '/v1/ignored'},
            ignored_path_prefixes={'/prefix-ignored'},
            allowed_oauth_client_ids={'fake_client_id'},
        )

    @pytest.mark.asyncio
    async def test_success(self, handler, middleware, request_obj, mock_get_user, expected_uid):
        await middleware(request_obj, handler)

        mock_get_user.assert_awaited_once()
        handler.assert_awaited_once_with(request_obj)
        assert_that(request_obj['user'].uid, equal_to(expected_uid))

    @pytest.mark.asyncio
    async def test_authenticator_init_call(self, handler, middleware, request_obj, mock_authenticator, mocker):
        await middleware(request_obj, handler)

        mock_authenticator.assert_called_once_with(
            client=mocker.ANY,
            scopes=[],
            authorization_header=request_obj.headers.get('Authorization'),
            session_id=request_obj.cookies.get('Session_id'),
            default_uid=request_obj.query.get('default_uid'),
            user_ip=request_obj.remote,
            host='ya.ru',
            logger=request_obj['logger'],
            allowed_oauth_client_ids={'fake_client_id'},
        )

    @pytest.mark.asyncio
    async def test_fail(self, handler, middleware, request_obj, mock_get_user):
        mock_get_user.side_effect = AuthenticationException

        with pytest.raises(AuthenticationException):
            await middleware(request_obj, handler)

    @pytest.mark.asyncio
    async def test_safe_method(self, handler, middleware, request_obj, mock_get_user):
        request_obj.method = hdrs.METH_OPTIONS

        await middleware(request_obj, handler)

        assert 'user' not in request_obj
        mock_get_user.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_skip_authentication(self, handler, middleware, request_obj, mock_get_user):
        handler = skip_authentication(handler)
        await middleware(request_obj, handler)

        assert 'user' not in request_obj
        mock_get_user.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_ignored_paths(self, handler, middleware, request_obj, mock_get_user):
        request_obj.path = '/ignored'

        await middleware(request_obj, handler)

        assert 'user' not in request_obj
        mock_get_user.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_ignored_path_prefix(self, handler, middleware, request_obj, mock_get_user):
        request_obj.path = '/prefix-ignored'

        await middleware(request_obj, handler)

        assert 'user' not in request_obj
        mock_get_user.assert_not_called()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_optional_authentication(self, handler, middleware, request_obj, mock_get_user):
        handler = optional_authentication(handler)
        mock_get_user.side_effect = AuthenticationException

        await middleware(request_obj, handler)

        assert 'user' not in request_obj
        mock_get_user.assert_awaited_once()
        handler.assert_awaited_once_with(request_obj)

    @pytest.mark.asyncio
    async def test_not_found(self, handler, middleware, request_obj, mock_get_user):
        request_obj.match_info = web_urldispatcher.MatchInfoError(Exception)
        await middleware(request_obj, handler)

        assert 'user' not in request_obj
        mock_get_user.assert_not_called()
        handler.assert_awaited_once_with(request_obj)
