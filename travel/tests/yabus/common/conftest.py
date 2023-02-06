# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import supplier_provider_patch


@pytest.fixture(scope="session", autouse=True)
def common_supplier_provider():
    with supplier_provider_patch("supplier_code"):
        yield
