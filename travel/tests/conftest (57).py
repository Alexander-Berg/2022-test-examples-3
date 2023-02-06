# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import os
import pytest

from travel.rasp.library.python.common23.tester.full_settings.default_conftest import *  # noqa

os.environ['DJANGO_SETTINGS_MODULE'] = 'travel.rasp.morda_backend.tests_settings'


@pytest.hookimpl(hookwrapper=True, tryfirst=True)
def pytest_runtest_setup(item):
    from travel.rasp.morda_backend.morda_backend.tariffs.train.base.utils import get_point_express_code

    get_point_express_code.cache_clear()

    yield
