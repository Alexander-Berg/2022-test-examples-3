import copy
from itertools import islice

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to

from mail.ipa.ipa.core.entities.collector import Collector
from mail.ipa.ipa.core.entities.user import User
from mail.ipa.ipa.storage.exceptions import UserAlreadyExists, UserNotFound


@pytest.fixture
def user_entity(org_id):
    return User(
        org_id=org_id,
        uid=111,
        login='userlogin',
    )


@pytest.fixture
async def user(storage, user_entity):
    return await storage.user.create(user_entity)


@pytest.fixture
async def users_with_collectors(org_id, create_user, create_collector):
    users = []

    for _ in range(3):
        for error in (None, 'some error'):
            user = await create_user(org_id, error=error)
            user.collector = None
            users.append(user)

    for _ in range(3):
        user = await create_user(org_id)
        for status in ('error status', Collector.OK_STATUS):
            collector = await create_collector(user_id=user.user_id, status=status)
            user_copy = copy.deepcopy(user)
            user_copy.collector = collector
            users.append(user_copy)

    return users


@pytest.fixture(autouse=True)
def func_now(mocker, make_now):
    now = make_now()
    mocker.patch('mail.ipa.ipa.storage.mappers.user.mapper.func.now', mocker.Mock(return_value=now))
    return now


@pytest.mark.asyncio
class TestCreate:
    async def test_create_new_user(self, storage, user_entity):
        user = await storage.user.create(user_entity)
        user_entity.user_id = user.user_id
        assert_that(user, equal_to(user_entity))

    async def test_create_user_already_exists(self, storage, user):
        with pytest.raises(UserAlreadyExists):
            await storage.user.create(user)


class TestGet:
    @pytest.mark.asyncio
    async def test_get(self, storage, user):
        actual_user = await storage.user.get(user.user_id)
        assert_that(actual_user, equal_to(user))

    @pytest.mark.asyncio
    async def test_get_not_found(self, storage):
        with pytest.raises(UserNotFound):
            await storage.user.get(1)


class TestDelete:
    @pytest.mark.asyncio
    async def test_delete(self, storage, user):
        await storage.user.delete(user)
        with pytest.raises(UserNotFound):
            await storage.user.get(1)


class TestFindOne:
    @pytest.mark.asyncio
    async def test_find_one(self, storage, user):
        found_user = await storage.user.find_one(login=user.login, org_id=user.org_id)
        assert_that(found_user.user_id, equal_to(user.user_id))

    @pytest.mark.asyncio
    async def test_find_one_raises_when_login_not_found(self, storage, user):
        with pytest.raises(UserNotFound):
            await storage.user.find_one(login=user.login + 'not-found', org_id=user.org_id)

    @pytest.mark.asyncio
    async def test_find_one_raises_when_org_id_not_found(self, storage, user):
        with pytest.raises(UserNotFound):
            await storage.user.find_one(login=user.login, org_id=user.org_id + 1)


@pytest.mark.asyncio
class TestGetOrCreate:
    async def test_get_or_create_new_user(self, storage, user_entity):
        new_user = await storage.user.get_or_create(user_entity)
        user_entity.user_id = new_user.user_id
        assert_that(new_user, equal_to(user_entity))

    async def test_get_or_create_when_user_already_exists(self, storage, user):
        assert_that(
            await storage.user.get_or_create(user),
            equal_to(user)
        )


@pytest.mark.asyncio
class TestFind:
    @pytest.fixture
    async def users(self, rands, org_id, other_org_id, create_user):
        return [
            await create_user(org_id=current_org_id, error=error)
            for current_org_id in (org_id, other_org_id)
            for error in (None, rands())
            for _ in range(3)
        ]

    @pytest.fixture
    def returned_func(self, storage, users):
        async def _inner(*args, **kwargs):
            return await alist(storage.user.find(*args, **kwargs))

        return _inner

    async def test_all(self, users, returned_func):
        assert_that(
            await returned_func(),
            contains_inanyorder(*users)
        )

    async def test_org_id_filter(self, org_id, users, returned_func):
        assert_that(
            await returned_func(org_id=org_id),
            contains_inanyorder(*[u for u in users if u.org_id == org_id])
        )

    @pytest.mark.parametrize('has_error', (False, True))
    async def test_has_error_filter(self, users, returned_func, has_error):
        assert_that(
            await returned_func(has_error=has_error),
            contains_inanyorder(*[u for u in users if (u.error is not None) == has_error])
        )

    @pytest.mark.parametrize('limit_delta', (-1, 0, 1))
    async def test_limit(self, users, returned_func, limit_delta):
        limit = len(users) + limit_delta
        assert len(await returned_func(limit=limit)) == min(len(users), limit)

    @pytest.mark.parametrize('offset', (0, 1))
    async def test_offset(self, users, returned_func, offset):
        assert len(await returned_func(offset=offset, limit=len(users))) == len(users[offset:])

    @pytest.mark.parametrize('desc', (False, True))
    async def test_order(self, users, returned_func, desc):
        assert await returned_func(order_by='user_id', desc=desc) == sorted(
            users,
            key=lambda user: user.user_id,
            reverse=desc,
        )


@pytest.mark.asyncio
class TestGetBatchWithCollectors:
    @pytest.fixture
    def returned_func(self, storage, org_id):
        async def _inner(org_id=org_id, **kwargs):
            return await alist(storage.user._get_batch_with_collectors(org_id=org_id, **kwargs))

        return _inner

    async def test_all(self, users_with_collectors, returned):
        assert returned == users_with_collectors

    async def test_org_id_filter(self, other_org_id, users_with_collectors, create_user, returned_func):
        await create_user(other_org_id)
        assert await returned_func() == users_with_collectors

    @pytest.mark.parametrize('limit_delta', (-1, 0, 1))
    async def test_limit(self, users_with_collectors, limit_delta, returned_func):
        limit = len(users_with_collectors) + limit_delta
        assert len(await returned_func(batch_size=limit)) == min(limit, len(users_with_collectors))

    @pytest.mark.parametrize('border_user_index', (0, 1, -1))
    async def test_id_filter(self, users_with_collectors, returned_func, border_user_index):
        border_user = users_with_collectors[border_user_index]
        assert await returned_func(
            user_id=border_user.user_id,
            collector_id=border_user.collector.collector_id if border_user.collector else None,
        ) == (users_with_collectors[border_user_index:])[1:]

    async def test_only_errors_filter(self, users_with_collectors, returned_func):
        assert await returned_func(only_errors=True) == [
            user
            for user in users_with_collectors
            if user.error is not None or (user.collector and user.collector.status != Collector.OK_STATUS)
        ]


class TestGetAllWithCollectors:
    @pytest.fixture(params=(1, 2, 3))
    def batch_size(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def get_batch_mock(self, mocker, storage, users_with_collectors):
        users_iter = iter(users_with_collectors)

        async def dummy_get_batch(org_id, user_id=None, collector_id=None, batch_size=None, only_errors=None):
            for user in islice(users_iter, batch_size):
                yield user

        return mocker.patch.object(
            storage.user,
            '_get_batch_with_collectors',
            mocker.Mock(side_effect=dummy_get_batch),
        )

    @pytest.fixture(params=(True, False))
    def only_errors(self, request):
        return request.param

    @pytest.fixture
    def returned_func(self, storage, org_id, batch_size, only_errors):
        async def _inner():
            return await alist(storage.user.get_all_with_collectors(
                org_id=org_id,
                batch_size=batch_size,
                only_errors=only_errors,
            ))

        return _inner

    def test_get_batch_calls(self,
                             mocker,
                             org_id,
                             users_with_collectors,
                             batch_size,
                             get_batch_mock,
                             only_errors,
                             returned,
                             ):
        assert get_batch_mock.call_args_list == [
            mocker.call(  # initial call without filter
                org_id=org_id,
                user_id=None,
                collector_id=None,
                batch_size=batch_size,
                only_errors=only_errors,
            )
        ] + [
            mocker.call(  # filter calls
                org_id=org_id,
                user_id=user.user_id,
                collector_id=user.collector.collector_id if user.collector else None,
                batch_size=batch_size,
                only_errors=only_errors,
            )
            for user in users_with_collectors[batch_size - 1::batch_size]
        ]

    def test_returned(self, users_with_collectors, returned):
        assert returned == users_with_collectors
