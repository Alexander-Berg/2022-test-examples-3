# coding: utf-8
from __future__ import unicode_literals

import pytest

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa

from common.tester.admin.django_auth import *  # noqa

from travel.library.python.resource import extract_resources


@pytest.hookimpl(tryfirst=True)
def pytest_configure(config):
    extract_resources("travel/rasp/suburban_selling/tests/")
    extract_resources("travel/rasp/suburban_selling/")
