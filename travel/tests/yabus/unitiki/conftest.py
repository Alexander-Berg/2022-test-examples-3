# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import converter_patch, supplier_provider_patch, \
    register_type_provider_patch


@pytest.fixture
def unitiki_converter():
    with converter_patch("unitiki"):
        yield


@pytest.fixture(scope="session", autouse=True)
def unitiki_supplier_provider():
    with supplier_provider_patch("unitiki-new"):
        yield


@pytest.fixture(scope="session", autouse=True)
def unitiki_register_type_provider():
    with register_type_provider_patch():
        yield
