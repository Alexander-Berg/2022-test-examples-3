import pytest

from crypta.profile.services.exports_based_on_socdem_or_lal.bin.test import api_response


pytest_plugins = [
    'crypta.lib.python.yt.test_helpers.fixtures',
]


@pytest.fixture(scope='function')
def common_example_lab_mock():
    with api_response.MockCryptaApi('common_example') as LabMock:
        yield LabMock


@pytest.fixture(scope='function')
def cycle_example_lab_mock():
    with api_response.MockCryptaApi('cycle_example') as LabMock:
        yield LabMock
