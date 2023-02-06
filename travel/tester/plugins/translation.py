# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.utils import translation


@pytest.hookimpl(hookwrapper=True, tryfirst=True)
def pytest_runtest_setup(item):
    translation.activate('ru')
    yield
