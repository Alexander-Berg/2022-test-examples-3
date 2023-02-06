import re

import pytest
from aiohttp import TCPConnector
from aioresponses import CallbackResult

from sendr_interactions.clients.blackbox import AbstractBlackBoxClient
from sendr_interactions.clients.blackbox.entities import (
    Email, Emails, EmailsMode, OauthResult, SessionIdResult, UIDData, UserInfo
)
from sendr_interactions.clients.blackbox.exceptions import (
    BlackBoxInvalidSessionError, UnknownBlackboxError, UserNotFoundBlackboxError
)
from sendr_utils import without_none

from hamcrest import assert_that, equal_to, has_entries


@pytest.fixture
def uid(request):
    return getattr(request, 'param', 1130000077777555)


@pytest.fixture
def status(request):
    return request.param


@pytest.fixture
def login_id():
    return 'some_test_login_id'


def create_blackbox_mock(aioresponses_mocker, path, payload):
    calls = []

    async def callback(url, **kwargs):
        calls.append({'url': url, 'params': kwargs.get('params', {}), 'headers': kwargs.get('headers', {})})
        return CallbackResult(
            status=200,
            payload=payload
        )

    aioresponses_mocker.get(path, callback=callback)
    return calls


@pytest.fixture
def mock_blackbox_session_response(aioresponses_mocker, status, uid, login_id):
    return create_blackbox_mock(
        aioresponses_mocker,
        re.compile('.*method=sessionid.*'),
        payload={'status': {'value': status}, 'uid': {'value': str(uid) if uid else None}, 'login_id': login_id},
    )


@pytest.fixture
def mock_blackbox_oauth_response(aioresponses_mocker, status, uid, login_id):
    return create_blackbox_mock(
        aioresponses_mocker,
        re.compile('.*method=oauth.*'),
        payload={
            'status': {'value': status},
            'oauth': {'client_id': 'client', 'uid': str(uid) if uid else None},
            'login_id': login_id,
        }
    )


class BlackBoxClient(AbstractBlackBoxClient):
    BASE_URL = 'https://blackbox.yandex.net/blackbox'
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


# TODO: затестить параметры, передаваемые блэкбоксу. Аналогично для остальных клиентов.
@pytest.fixture
async def blackbox_client(create_interaction_client):
    client = create_interaction_client(BlackBoxClient)
    yield client
    await client.close()


@pytest.mark.asyncio
@pytest.mark.usefixtures('mock_blackbox_session_response')
@pytest.mark.parametrize('status', ('VALID', 'NEED_RESET'), indirect=True)
async def test_get_session_response(status, blackbox_client, aioresponses_mocker, uid, login_id):
    user = await blackbox_client.get_session_info(
        session_id='asd',
        host='yandex.ru',
        user_ip='192.0.2.1',
    )
    assert_that(user, equal_to(SessionIdResult(uid=uid, login_id=login_id, is_yandexoid=False)))


@pytest.mark.asyncio
@pytest.mark.parametrize('status', ('EXPIRED', 'INVALID', 'NOAUTH', 'DISABLED'), indirect=True)
async def test_invalid_session_response(aioresponses_mocker, blackbox_client, status):
    error = 'signature has bad format or is broken'
    aioresponses_mocker.get(
        re.compile('.*method=sessionid.*'),
        status=200,
        payload={'status': {'value': status, 'id': 5}, 'error': error, 'uid': {'value': 12345}}
    )

    with pytest.raises(BlackBoxInvalidSessionError) as exc_info:
        await blackbox_client.get_session_info(
            session_id='asd',
            host='yandex.ru',
            user_ip='192.0.2.1',
        )

    assert_that(exc_info.value.params, equal_to({'error': error, 'status': status}))


@pytest.mark.asyncio
@pytest.mark.parametrize('status', ('VALID', 'NEED_RESET'), indirect=True)
@pytest.mark.parametrize('uid', (123, None), indirect=True)
@pytest.mark.usefixtures('mock_blackbox_oauth_response')
async def test_get_oauth_token_info(blackbox_client, uid, login_id):
    user = await blackbox_client.get_oauth_token_info(
        oauth_token='asd',
        user_ip='192.0.2.1',
    )
    expected = OauthResult(
        client_id='client',
        uid=uid,
        login_id=login_id,
        is_yandexoid=False,
    )
    assert_that(user, equal_to(expected))


@pytest.mark.asyncio
@pytest.mark.parametrize('status', ('VALID', 'NEED_RESET'), indirect=True)
@pytest.mark.parametrize('client_params, expected_aiohttp_params', (
    (
        {'user_ip': '192.0.2.1', 'oauth_token': 'asd'},
        {'userip': '192.0.2.1', 'oauth_token': 'asd', 'method': 'oauth', 'format': 'json', 'get_login_id': 'yes',
         'aliases': '13'}
    ),
    (
        {'user_ip': '192.0.2.1', 'oauth_token': 'asd', 'scopes': ['a', 'b']},
        {
            'userip': '192.0.2.1',
            'oauth_token': 'asd',
            'method': 'oauth',
            'format': 'json',
            'scopes': 'a,b',
            'get_login_id': 'yes',
            'aliases': '13',
        }
    ),
))
async def test_get_oauth_token_info_params(
    blackbox_client,
    client_params,
    expected_aiohttp_params,
    mock_blackbox_oauth_response,
):
    await blackbox_client.get_oauth_token_info(**client_params)

    assert len(mock_blackbox_oauth_response) == 1

    call_kwargs = mock_blackbox_oauth_response[0]
    assert_that(call_kwargs, has_entries(params=expected_aiohttp_params))


@pytest.mark.asyncio
@pytest.mark.parametrize('status', ('VALID', 'NEED_RESET'), indirect=True)
@pytest.mark.parametrize('client_params, expected_aiohttp_params', (
    (
        {'user_ip': '192.0.2.1', 'host': 'asd', 'session_id': 'session'},
        {'userip': '192.0.2.1', 'host': 'asd', 'sessionid': 'session', 'method': 'sessionid',
         'format': 'json', 'get_login_id': 'yes',
         'aliases': '13'}
    ),
    (
        {'user_ip': '192.0.2.1', 'host': 'asd', 'session_id': 'session', 'default_uid': 10},
        {
            'userip': '192.0.2.1',
            'host': 'asd',
            'sessionid': 'session',
            'default_uid': 10,
            'method': 'sessionid',
            'format': 'json',
            'get_login_id': 'yes',
            'aliases': '13',
        }
    ),
))
async def test_get_session_info_params(
    blackbox_client,
    client_params,
    expected_aiohttp_params,
    mock_blackbox_session_response,
):
    await blackbox_client.get_session_info(**client_params)

    assert len(mock_blackbox_session_response) == 1

    call_kwargs = mock_blackbox_session_response[0]
    assert_that(call_kwargs, has_entries(params=expected_aiohttp_params))


@pytest.mark.asyncio
async def test_get_user_info(aioresponses_mocker, blackbox_client):
    calls = create_blackbox_mock(
        aioresponses_mocker,
        re.compile(r'.*/blackbox\?.*'),
        payload={'users': [{'id': '123456789', 'uid': {'value': '123456789'}, 'attributes': {'1337': '1'}}]}
    )
    user_ip = '127.0.0.1'
    response = await blackbox_client.get_user_info(
        uid=123456789,
        user_ip=user_ip,
        attributes=['xxx', 'yyy']
    )

    assert_that(response, equal_to(UserInfo(uid_data=UIDData(value=123456789), attributes={'1337': '1'})))
    assert_that(response.uid, equal_to(123456789))

    assert len(calls) == 1
    assert_that(
        calls[0],
        has_entries(
            params={
                'method': 'userinfo',
                'uid': '123456789',
                'format': 'json',
                'attributes': 'xxx,yyy',
                'userip': user_ip,
            },
        )
    )


class TestGetUserInfoEmails:
    @pytest.fixture(autouse=True)
    def mock_blackbox(self, aioresponses_mocker):
        return create_blackbox_mock(
            aioresponses_mocker,
            re.compile(r'.*/blackbox\?.*'),
            payload={
                'users': [{
                    'id': '123456789',
                    'uid': {'value': '123456789'},
                    'address-list': [{'address': 'email@test', 'default': True, 'native': True}]
                }]
            }
        )

    @pytest.mark.asyncio
    async def test_response(self, aioresponses_mocker, blackbox_client):
        response = await blackbox_client.get_user_info(
            uid=123456789,
            user_ip='127.0.0.1',
            emails_mode=EmailsMode.DEFAULT,
        )

        assert_that(
            response,
            equal_to(
                UserInfo(
                    uid_data=UIDData(value=123456789),
                    address_list=[Email(address='email@test', default=True, native=True)],
                )
            )
        )
        assert_that(
            response.emails, equal_to(Emails(address_list=[Email(address='email@test', default=True, native=True)]))
        )
        assert_that(response.emails.default, equal_to(Email(address='email@test', default=True, native=True)))

    @pytest.mark.parametrize('emails_mode', EmailsMode)
    @pytest.mark.asyncio
    async def test_call(self, mock_blackbox, blackbox_client, emails_mode):
        expected_emails = {
            EmailsMode.DEFAULT: 'getdefault',
            EmailsMode.ALL: 'getall',
            EmailsMode.NONE: None,
        }

        await blackbox_client.get_user_info(
            uid=123456789,
            user_ip='127.0.0.1',
            emails_mode=emails_mode,
        )

        assert len(mock_blackbox) == 1
        call_kwargs = mock_blackbox[0]
        assert_that(
            call_kwargs,
            has_entries(
                params=without_none({
                    'method': 'userinfo',
                    'uid': '123456789',
                    'format': 'json',
                    'emails': expected_emails[emails_mode],
                    'userip': '127.0.0.1',
                }),
            )
        )


@pytest.mark.asyncio
async def test_get_user_info__when_not_found__raises_error(aioresponses_mocker, blackbox_client):
    aioresponses_mocker.get(
        re.compile(r'.*/blackbox\?.*'),
        payload={'users': [{'id': '1234567890', 'uid': {}, 'attributes': {}}]}
    )

    with pytest.raises(UserNotFoundBlackboxError):
        await blackbox_client.get_user_info(
            uid=1234567890,
            user_ip='127.0.0.1',
            attributes=['xxx', 'yyy']
        )


@pytest.mark.asyncio
async def test_get_user_info__when_response_is_empty__raises_unknown_error(aioresponses_mocker, blackbox_client):
    aioresponses_mocker.get(
        re.compile(r'.*/blackbox\?.*'),
        payload={}
    )

    with pytest.raises(UnknownBlackboxError):
        await blackbox_client.get_user_info(
            uid=1234567890,
            user_ip='127.0.0.1',
            attributes=['xxx', 'yyy']
        )


@pytest.mark.parametrize('attrs, expected', (
    ({'1015': 'whatever'}, True),
    ({'1016': 'whatever'}, False),
    ({}, False),
))
@pytest.mark.asyncio
async def test_have_plus(aioresponses_mocker, blackbox_client, attrs, expected):
    aioresponses_mocker.get(
        re.compile(r'.*/blackbox\?.*'),
        payload={'users': [{'id': '123456789', 'uid': {'value': '123456789'}, 'attributes': attrs}]}
    )

    response = await blackbox_client.have_plus(
        uid=123456789,
        user_ip='127.0.0.1',
    )

    assert_that(response, equal_to(expected))


@pytest.mark.asyncio
async def test_should_return_yandexoid_on_get_oauth(aioresponses_mocker, blackbox_client):
    aioresponses_mocker.get(
        re.compile('.*method=oauth.*'),
        payload={
            'status': {'value': 'VALID'},
            'oauth': {'uid': 444, 'client_id': 'client'},
            'login_id': 'login_id',
            'aliases': {'13': ''},
        },
    )

    blackbox_result = await blackbox_client.get_oauth_token_info(oauth_token='token', user_ip='1.1.1.1')

    assert blackbox_result.is_yandexoid


@pytest.mark.asyncio
async def test_should_return_yandexoid_on_get_session(aioresponses_mocker, blackbox_client):
    aioresponses_mocker.get(
        re.compile('.*method=sessionid.*'),
        payload={'status': {'value': 'VALID'}, 'uid': {'value': 33}, 'login_id': 'login_id',
                 'aliases': {'13': ''}},
    )

    blackbox_result = await blackbox_client.get_session_info(host='test', session_id='id', user_ip='1.1.1.1')

    assert blackbox_result.is_yandexoid
