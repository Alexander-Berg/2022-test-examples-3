import os
from copy import deepcopy

import aiohttp.pytest_plugin
import pytest

from sendr_pytest import *  # noqa

from mail.beagle.beagle.api.app import BeagleApplication
from mail.beagle.beagle.storage import Storage

pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(autouse=True)
def env(randn, rands):
    os.environ['QLOUD_TVM_TOKEN'] = rands()


@pytest.fixture
def beagle_settings():
    from mail.beagle.beagle.conf import settings
    data = deepcopy(settings._settings)
    yield settings
    settings._settings = data


@pytest.fixture
async def app(aiohttp_client, db_engine):
    return await aiohttp_client(BeagleApplication(db_engine=db_engine))


@pytest.fixture
async def storage(app, db_engine):
    # app dependency is required to ensure exit order
    async with db_engine.acquire() as conn:
        yield Storage(conn)


@pytest.fixture
async def returned(returned_func):
    return await returned_func()
