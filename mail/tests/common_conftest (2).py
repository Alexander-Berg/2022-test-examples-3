import os

import aiohttp.pytest_plugin
import pytest
import pytz
import ujson

from sendr_pytest import *  # noqa
from sendr_utils import temp_set

from mail.ciao.ciao.api.app import CiaoApplication


def pytest_configure(config):
    os.environ['QLOUD_TVM_TOKEN'] = 'x' * 32
    os.environ['QLOUD_TVM_CONFIG'] = ujson.dumps({
        'BbEnvType': 1,
        'clients': {
            'ciao': {
                'secret': 'tvm_secret',
                'self_tvm_id': 0,
                'dsts': {},
            },
        },
    })


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def app(mocker, aiohttp_client):
    mocker.patch.object(CiaoApplication, 'add_schedule', mocker.Mock())
    return await aiohttp_client(CiaoApplication())


@pytest.fixture
def mock_action(mocker):
    def _inner(action_cls, action_result=None):
        async def run(self):
            return action_result

        mocker.patch.object(action_cls, 'run', run)
        return mocker.patch.object(action_cls, '__init__', mocker.Mock(return_value=None))
    return _inner


@pytest.fixture
async def returned(returned_func):
    return await returned_func()


@pytest.fixture
def settings():
    from mail.ciao.ciao.conf import settings
    return settings


@pytest.fixture
def user(randn, rands):
    from mail.ciao.ciao.core.entities.user import User
    return User(
        uid=randn(),
        timezone=pytz.timezone('Australia/Sydney'),  # Using unusual timezone to find bugs faster
        user_ticket=rands(),
    )


@pytest.fixture
def setup_user(user):
    from mail.ciao.ciao.core.context import CORE_CONTEXT
    with temp_set(CORE_CONTEXT, 'user', user):
        yield
