import pytest

from crypta.profile.services.socdem_expressions_for_direct.bin.test import api_response


pytest_plugins = [
    'crypta.lib.python.yt.test_helpers.fixtures',
]


@pytest.fixture(scope='function')
def common_example_lab_mock():
    with api_response.MockCryptaApi('/common_example.json') as LabMock:
        yield LabMock


@pytest.fixture(scope='function')
def errors_example_lab_mock():
    with api_response.MockCryptaApi('/errors_example.json') as LabMock:
        yield LabMock


@pytest.fixture(scope='function')
def max_vars_diff_example_lab_mock():
    with api_response.MockCryptaApi('/max_vars_diff_example.json') as LabMock:
        yield LabMock
