import os

import aiohttp.pytest_plugin
import pytest

from {{cookiecutter.import_path}}.api.app import {{cookiecutter.ProjectName}}Application
from {{cookiecutter.import_path}}.storage import Storage


def pytest_configure(config):
    os.environ['TVMTOOL_LOCAL_AUTHTOKEN'] = 'xxxxxxxxxxx'


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def app(aiohttp_client, db_engine):
    return await aiohttp_client({{cookiecutter.ProjectName}}Application(db_engine=db_engine))


@pytest.fixture
def mock_action(mocker):
    def _inner(action_cls, action_result=None):
        async def run(self):
            return action_result

        mocker.patch.object(action_cls, 'run', run)
        return mocker.patch.object(action_cls, '__init__', mocker.Mock(return_value=None))
    return _inner


@pytest.fixture
async def dbconn(app, db_engine):
    # app dependency is required to ensure exit order
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
def storage(dbconn) -> Storage:
    return Storage(dbconn)
