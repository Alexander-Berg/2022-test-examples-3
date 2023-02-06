# -*- coding: utf-8 -*-
import pytest
from runtime_tests.util.connection import HTTPConnectionManager


pytest_plugins = [
    'runtime_tests.plugin.resource',
    'runtime_tests.plugin.stream',
]


@pytest.fixture(scope='session')
def session_connection_manager(session_resource_manager, session_stream_manager):
    return HTTPConnectionManager(session_resource_manager, session_stream_manager)
