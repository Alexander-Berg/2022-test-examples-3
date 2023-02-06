import math
from itertools import count

import pytest

from hamcrest import assert_that, contains, has_entries, has_items

from mail.beagle.beagle.interactions.blackbox import (
    BlackBoxDefaultEmailNotFoundError, BlackBoxUserException, UnknownBlackBoxException, UserInfo
)
from mail.beagle.beagle.utils.helpers import without_none


class BaseUserInfo:
    @pytest.fixture
    def uids(self):
        return None

    @pytest.fixture
    def default_email(self, randmail):
        return randmail()

    @pytest.fixture
    def login(self):
        return None

    @pytest.fixture
    def userip(self, rands):
        return rands()

    @pytest.fixture
    def chunk_size(self, uids):
        return 1

    @pytest.fixture(params=(True, False))
    def get_default_emails(self, request):
        return request.param

    @pytest.fixture
    def skip_exceptions(self):
        return True

    @pytest.fixture
    def response(self):
        raise NotImplementedError

    @pytest.fixture(autouse=True)
    def setup(self, mock_blackbox, response):
        mock_blackbox(response)

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_params(self, returned, login, uids, userip, last_blackbox_request, get_default_emails,
                          expected_returned):
        request = last_blackbox_request()

        assert_that(
            (request.query, returned),
            contains(
                has_entries(
                    without_none({
                        'method': 'userinfo',
                        'format': 'json',
                        'userip': userip,
                        'uid': ','.join(map(str, uids)) if uids else None,
                        'login': login,
                        'emails': 'getdefault' if get_default_emails else None
                    })
                ),
                expected_returned
            )
        )

    class TestDefaultEmailException:
        @pytest.fixture
        def skip_exceptions(self):
            return False

        @pytest.fixture
        def default_email(self):
            return None

        @pytest.fixture
        def get_default_emails(self):
            return True

        @pytest.mark.asyncio
        async def test_default_email_exception(self, returned_func):
            with pytest.raises(BlackBoxDefaultEmailNotFoundError):
                await returned_func()

    class TestUserException:
        @pytest.fixture
        def skip_exceptions(self):
            return False

        @pytest.fixture
        def response(self, uids, rands):
            raise NotImplementedError

        @pytest.mark.asyncio
        async def test_user_exception(self, returned_func):
            with pytest.raises(BlackBoxUserException):
                await returned_func()

    class TestUnknownException:
        @pytest.fixture
        def response(self):
            return {"exception": {"value": "DB_EXCEPTION", "id": 10}}

        @pytest.mark.asyncio
        async def test_unknown_exception(self, returned_func):
            with pytest.raises(UnknownBlackBoxException):
                await returned_func()


class TestUserInfoByUids(BaseUserInfo):
    @pytest.fixture
    def uids(self, randn):
        return [randn() for _ in range(randn(min=2, max=16))]

    @pytest.fixture
    def expected_returned(self, response, get_default_emails):
        return has_items(*[
            UserInfo(
                uid=user['id'],
                default_email=user['address-list'][0]['address'] if get_default_emails else None
            )
            for i, user in zip(count(), response['users']) if not get_default_emails or i % 2 == 0
        ])

    @pytest.fixture
    def returned_func(self, uids, clients, userip, skip_exceptions, chunk_size, get_default_emails):
        async def _inner():
            return await clients.blackbox.userinfo_by_uids(
                uids,
                userip=userip,
                get_default_emails=get_default_emails,
                chunk_size=chunk_size,
                skip_exceptions=skip_exceptions
            )

        return _inner

    @pytest.fixture
    def chunk_size(self, uids):
        return len(uids)

    @pytest.fixture(autouse=True)
    def setup(self, mock_blackbox, uids, chunk_size, response):
        for _ in range(math.ceil(len(uids) / chunk_size)):
            mock_blackbox(response)

    @pytest.fixture
    def response(self, uids, randn, randmail):
        return {
            'users': [
                {
                    'id': uid,
                    'address-list': [
                        without_none({'default': True if i % 2 == 0 else None, 'address': randmail()})
                    ]
                }
                for i, uid in zip(count(), uids)
            ]
        }

    class TestUserException(BaseUserInfo.TestUserException):
        @pytest.fixture
        def response(self, rands, uids):
            return {
                'users': [
                    {
                        "exception": {"value": "DB_EXCEPTION", "id": 10},
                        "error": rands(),
                        "id": uid
                    } for uid in uids
                ]
            }

    @pytest.mark.asyncio
    @pytest.mark.parametrize('chunk_size', (1,))
    async def test_chunk_size(self, uids, returned, blackbox_requests, chunk_size):
        assert len(blackbox_requests()) == len(uids)


class BaseTestUserInfoBy(BaseUserInfo):
    @pytest.fixture
    def expected_returned(self, response, get_default_emails):
        user = response['users'][0]
        return UserInfo(
            uid=user['id'],
            default_email=user['address-list'][0]['address'] if get_default_emails else None
        )

    @pytest.fixture
    def response(self, uids, randn, default_email, randmail):
        return {
            'users': [
                {
                    'id': randn(),
                    'address-list': [
                        {'default': default_email, 'address': randmail()}
                    ]
                }
            ]
        }

    class TestUserException(BaseUserInfo.TestUserException):
        @pytest.fixture
        def response(self, rands, randn):
            return {
                'users': [
                    {
                        "exception": {"value": "DB_EXCEPTION", "id": 10},
                        "error": rands(),
                        "id": randn()
                    }
                ]
            }


class TestUserInfoByUid(BaseTestUserInfoBy):
    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture
    def returned_func(self, uid, clients, userip, get_default_emails):
        async def _inner():
            return await clients.blackbox.userinfo_by_uid(
                uid,
                userip=userip,
                get_default_emails=get_default_emails,
            )

        return _inner


class TestUserInfoByLogin(BaseTestUserInfoBy):
    @pytest.fixture
    def login(self, rands):
        return rands()

    @pytest.fixture
    def returned_func(self, login, clients, userip, get_default_emails):
        async def _inner():
            return await clients.blackbox.userinfo_by_login(
                login,
                userip=userip,
                get_default_emails=get_default_emails,
            )

        return _inner
