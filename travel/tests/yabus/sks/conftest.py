# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import register_type_provider_patch


@pytest.fixture(scope="session", autouse=True)
def sks_register_type_provider():
    with register_type_provider_patch():
        yield
