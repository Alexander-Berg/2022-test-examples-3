# -*- coding: utf-8 -*-
import os

import django
import mock
import pytest
from faker import Factory

os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.avia.ticket_daemon.tests.tests_settings'
django.setup()

from travel.avia.library.python.tester import hacks
hacks.apply_format_explanation()

pytest_plugins = [
    'travel.avia.library.python.tester.initializer',
    'travel.avia.library.python.tester.plugins.transaction',
]


@pytest.fixture(scope='session', autouse=True)
def feature_flags_patching_fixture(request):
    """
    :type request: _pytest.python.SubRequest
    :return:
    """
    from feature_flag_client import Context
    enabled_flags = set()

    with mock.patch(
        'feature_flag_client.Client.create_context',
        return_value=Context(enabled_flags)
    ):
        yield


@pytest.fixture(scope='session', autouse=True)
def yt_client_patching_fixture(request):
    """
    Блокируем глобально YT-клиента для всех тестов
    :type request: _pytest.python.SubRequest
    :return:
    """
    with mock.patch('yt.wrapper.YtClient'):
        yield


@pytest.fixture(scope='session')
def faker():
    return Factory.create()
