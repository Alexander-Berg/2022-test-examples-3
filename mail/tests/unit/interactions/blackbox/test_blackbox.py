import pytest
from sendr_writers.base.pusher import InteractionResponseLog

from hamcrest import all_of, assert_that, has_properties, is_

from mail.payments.payments.interactions.exceptions import BlackBoxUserNotFoundError
from mail.payments.payments.utils.helpers import without_none


@pytest.mark.parametrize(['uids', 'result'], [
    [1, '1'],
    ['2', '2'],
    [(1, 2), '1,2'],
    [None, None],
    [(1, '2'), '1,2'],
    [('1', '2'), '1,2'],
    [(), None]
])
def test_uid_param_method(blackbox_client, uids, result):
    """Accepts int, str or iterable of str or int values (empty iterables treated as Nones)"""
    assert blackbox_client._uid_param(uids) == result


class TestUserInfoMethod:
    @pytest.fixture
    def login(self):
        return 'login'

    @pytest.fixture
    def uid(self, unique_rand, randn):
        return unique_rand(randn, basket='uid')

    @pytest.fixture
    def userinfo(self, login, uid):
        return [
            {
                'id': str(uid),
                'uid': {'value': str(uid), 'lite': False, 'hosted': False},
                'login': login,
                'have_password': True,
                'have_hint': True,
                'karma': {'value': 0},
                'karma_status': {'value': 0}
            }
        ]

    @pytest.fixture
    def response_json(self, userinfo):
        return {
            'users': userinfo,
        }

    @pytest.fixture
    def kwargs(self, login):
        return {
            'login': login,
        }

    @pytest.mark.parametrize('userinfo', [
        [{'id': 1}], [{'id': 1}, {'id': 2}]
    ])
    @pytest.mark.asyncio
    async def test_returns_users_field_from_json_response(self, blackbox_client, kwargs, userinfo):
        """Given response JSON returns "users" field"""
        assert userinfo[0] == (await blackbox_client._userinfo(**kwargs))

    @pytest.mark.asyncio
    async def test_bb_exception(self, response_json, kwargs, blackbox_client):
        response_json.pop('users', None)
        response_json.update({'exception': {}, 'error': 'error'})
        with pytest.raises(BlackBoxUserNotFoundError):
            await blackbox_client._userinfo(**kwargs)

    @pytest.mark.asyncio
    async def test_empty_users(self, response_json, kwargs, blackbox_client):
        response_json.update({'users': []})
        with pytest.raises(BlackBoxUserNotFoundError):
            await blackbox_client._userinfo(**kwargs)

    @pytest.mark.asyncio
    async def test_empty_id(self, response_json, kwargs, blackbox_client):
        response_json.update({'users': [{'id': ''}]})
        with pytest.raises(BlackBoxUserNotFoundError):
            await blackbox_client._userinfo(**kwargs)

    @pytest.mark.asyncio
    async def test_make_request_call_args(self, payments_settings, blackbox_client, kwargs, userinfo):
        """Calls make_request with proper args and kwargs"""
        await blackbox_client._userinfo(**kwargs)
        call_args = ('userinfo', 'GET', payments_settings.BLACKBOX_API_URL)
        assert blackbox_client.call_args == call_args

    @pytest.mark.asyncio
    async def test_make_request_call_kwargs(self, blackbox_client, kwargs, userinfo):
        await blackbox_client._userinfo(**kwargs)
        expected_get_params = without_none({
            'method': 'userinfo',
            'format': 'json',
            'userip': kwargs.get('userip', '127.0.0.1'),
            'sid': kwargs.get('sid'),
            'uid': kwargs.get('uid'),
            'login': kwargs.get('login'),
        })
        assert blackbox_client.call_kwargs == {'params': expected_get_params}

    @pytest.mark.asyncio
    async def test_uid(self, blackbox_client, login, userinfo):
        assert int(userinfo[0]['id']) == (await blackbox_client.userinfo(login=login)).uid

    @pytest.mark.asyncio
    async def test_response_logged(self, blackbox_client, kwargs, pushers_mock, response_json):
        await blackbox_client._userinfo(**kwargs)
        assert_that(
            pushers_mock.response_log.push.call_args[0][0],
            all_of(
                is_(InteractionResponseLog),
                has_properties(dict(
                    response=response_json,
                    request_url=blackbox_client.call_args[2],
                    request_method=blackbox_client.call_args[1],
                    request_kwargs=blackbox_client.call_kwargs,
                    request_id=blackbox_client.request_id,
                    status=200,
                ))
            ),
        )
