# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest


@pytest.hookimpl(hookwrapper=True, tryfirst=True)
def pytest_runtest_setup(item):
    from travel.rasp.library.python.common23.models.core.geo.country import get_country_capital
    from travel.rasp.library.python.common23.date.date import get_timezone_from_point

    get_country_capital.cache_clear()
    get_timezone_from_point.cache_clear()

    yield
