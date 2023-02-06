# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa


def pytest_configure(config):
    from common.tester.initializer import CONFIG as INITIALIZER_CONFIG

    INITIALIZER_CONFIG['auto_create_objects'] = []


def pytest_sessionstart(session):
    from django.conf import settings
    assert settings.RZD_DATABASE_NAME == '__rasp-tests__'
    assert settings.ENABLE_GET_RZD_HOSTS_FROM_YP is False
