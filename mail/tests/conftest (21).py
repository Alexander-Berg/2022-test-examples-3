import os

import pytest

from sendr_pytest import coromock  # noqa

YA_TEST_RUNNER = os.environ.get('YA_TEST_RUNNER') == '1'

if YA_TEST_RUNNER:
    import aiohttp.pytest_plugin  # noqa

    pytestmark = pytest.mark.asyncio

    pytest_plugins = ['aiohttp.pytest_plugin']
    del aiohttp.pytest_plugin.loop

    @pytest.fixture
    def loop(event_loop):
        return event_loop
