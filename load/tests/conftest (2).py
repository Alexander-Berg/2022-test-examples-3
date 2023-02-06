import shutil
import tempfile
import aiohttp.pytest_plugin

import pytest

from load.projects.cloud.loadtesting.tools.logging.storage.src.storage import create_app

pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    """
    не разбирался как это работает.
    вместе с `del aiohttp.pytest_plugin.loop` стырено отсюда:
    https://a.yandex-team.ru/arc/trunk/arcadia/ads/emily/viewer/backend/tests/conftest.py?rev=r8704789#L23
    но без этого плагин 'aiohttp.pytest_plugin' не завёлся.
    """
    return event_loop


@pytest.fixture()
def dir_path():
    dirpath = tempfile.mkdtemp()
    try:
        yield dirpath
    finally:
        shutil.rmtree(dirpath)


@pytest.fixture
async def client(aiohttp_client, dir_path):
    return await aiohttp_client(create_app(dir_path))
