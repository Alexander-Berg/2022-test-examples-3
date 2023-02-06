import os
from copy import deepcopy
from datetime import datetime, timedelta, timezone

import aiohttp.pytest_plugin
import pytest

from sendr_pytest import *  # noqa

from mail.ipa.ipa.api.app import IpaApplication
from mail.ipa.ipa.storage import Storage
from mail.ipa.ipa.tests.utils import Holder


@pytest.fixture(autouse=True)
def env():
    os.environ['QLOUD_TVM_TOKEN'] = 'tvm_token'


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
def ipa_settings():
    from mail.ipa.ipa.conf import settings
    data = deepcopy(settings._settings)
    yield settings
    settings._settings = data


@pytest.fixture
async def app(aiohttp_client, db_engine, ipa_settings):
    ipa_settings.TVM_CHECK_SERVICE_TICKET = False
    return await aiohttp_client(IpaApplication(db_engine=db_engine))


@pytest.fixture
async def dbconn(app, db_engine):
    # app dependency is required to ensure exit order
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
async def storage(dbconn):
    return Storage(dbconn)


@pytest.fixture
def rand_org_id(unique_rand, randn):
    def _inner():
        return unique_rand(randn, basket='org_id')

    return _inner


@pytest.fixture
def org_id(rand_org_id):
    return rand_org_id()


@pytest.fixture
def other_org_id(rand_org_id):
    return rand_org_id()


@pytest.fixture
def admin_uid():
    return 113001


@pytest.fixture
def user_ip():
    return '127.0.0.1'


@pytest.fixture
async def returned(returned_func):
    return await returned_func()


@pytest.fixture
def suid():
    return 15151515


@pytest.fixture
def pop_id():
    return 'popid'


@pytest.fixture
def mock_encryptor_iv(mocker):
    from mail.ipa.ipa.core.crypto.base import BlockEncryptor
    # Мокаем iv для предсказуемости.
    # А нужно это потому что, например, Password('123').encrypted() != Password('123').encrypted()
    # Ведь iv каждый раз случайный. Для тестов это может быть неудобно.
    iv = BlockEncryptor._generate_iv()
    mocker.patch.object(BlockEncryptor, '_generate_iv', mocker.Mock(return_value=iv))


@pytest.fixture
def worker_app(mocker, db_engine):
    return mocker.Mock(db_engine=db_engine)


@pytest.fixture
def make_now():
    return lambda: datetime.now(tz=timezone.utc)


@pytest.fixture
def past_time(make_now):
    return make_now() - timedelta(hours=1)


@pytest.fixture
async def hold(mocker):
    holders = []

    def _hold(obj, attr):
        nonlocal holder

        func = getattr(obj, attr)
        holder = Holder(func)
        holder.mock = mocker.patch.object(obj, attr, holder)

        holders.append(holder)

        return holder

    yield _hold
    for holder in holders:
        if not holder.released:
            holder.release()


@pytest.fixture
def coromock(mocker):
    def _inner(result=None, exc=None):
        async def coro(*args, **kwargs):
            if exc:
                raise exc
            return result

        return mocker.Mock(side_effect=coro)

    return _inner


@pytest.fixture
def enforce_action_lock_timeout(mocker):
    # milliseconds = 1 означает, что любая блокировка (Lock) практически сразу повалит
    # транзакцию. Не стоит опираться в тестах на то, что это произойдёт быстро.
    # Такой timeout выставлен не для этого. Он выставлен таким, чтобы тест не бежал долго.
    def _enforce_action_lock_timeout(action_class, milliseconds=1):
        orig_handle = action_class.handle

        async def handle_with_lock(self):
            await self.storage.conn.execute(f"set lock_timeout = '{milliseconds}ms'")
            return await orig_handle.__get__(self)()

        mocker.patch.object(action_class, 'handle', handle_with_lock)

    return _enforce_action_lock_timeout
