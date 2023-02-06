# -*- coding: utf-8 -*-
import pytest
from runtime_tests.util.backend import BackendManager


pytest_plugins = [
    'runtime_tests.plugin.logger',
    'runtime_tests.plugin.resource',
    'runtime_tests.plugin.stream',
]


@pytest.fixture
def backend_manager(logger, resource_manager, stream_manager):
    return BackendManager(logger, resource_manager, stream_manager)
