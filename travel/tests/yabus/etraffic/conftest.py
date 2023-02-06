# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import supplier_provider_patch, converter_patch
from yabus.etraffic import SoapClient, client


@pytest.fixture
def etraffic_client():
    with mock.patch.object(SoapClient, "__init__"), mock.patch.object(SoapClient, "call", return_value=[]) as m_call:
        result = client.Client(spawn_segments_provider=False)
        m_call.reset_mock()
        yield result


@pytest.fixture
def etraffic_converter():
    with converter_patch("etraffic"):
        yield


@pytest.fixture(scope="session", autouse=True)
def etraffic_supplier_provider():
    with supplier_provider_patch("etraffic"):
        yield
