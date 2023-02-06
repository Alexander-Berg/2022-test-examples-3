# -*- coding: utf-8 -*-
import pytest
from runtime_tests.util.stream import StreamManager


pytest_plugins = [
    'runtime_tests.plugin.resource',
]


@pytest.fixture(scope='session')
def session_stream_manager(session_resource_manager):
    return StreamManager(session_resource_manager)


@pytest.fixture
def stream_manager(resource_manager):
    return StreamManager(resource_manager)
