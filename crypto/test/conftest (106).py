import pytest


pytest_plugins = [
    'crypta.lib.python.nirvana.test_helpers.fixtures',
]


@pytest.fixture
def date():
    return '2022-02-24'
