import os

import aiohttp.pytest_plugin
import pytest

YA_TEST_RUNNER = os.environ.get('YA_TEST_RUNNER') == '1'

pytestmark = pytest.mark.asyncio

if YA_TEST_RUNNER:
    pytest_plugins = ['aiohttp.pytest_plugin']
    del aiohttp.pytest_plugin.loop

    @pytest.fixture
    def loop(event_loop):
        return event_loop
