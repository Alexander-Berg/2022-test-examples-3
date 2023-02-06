# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import converter_patch, supplier_provider_patch


@pytest.fixture
def busfor_converter():
    with converter_patch("busfor"):
        yield


@pytest.fixture(scope="session", autouse=True)
def busfor_supplier_provider():
    with supplier_provider_patch("busfor"):
        yield
