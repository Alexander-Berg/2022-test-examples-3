# -*- coding: utf-8 -*-
import pytest

from runtime_tests.util.resource import AbstractResourceManager


class PytestResourceManager(AbstractResourceManager):
    def __init__(self, request):
        super(PytestResourceManager, self).__init__()
        request.addfinalizer(self._finish_all)


@pytest.fixture(scope='session')
def session_resource_manager(request):
    return PytestResourceManager(request)


@pytest.fixture
def resource_manager(request):
    return PytestResourceManager(request)
