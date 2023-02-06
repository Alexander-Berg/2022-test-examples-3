import pytest

pytest_plugins = [
    'crypta.lib.python.chyt.test_helpers.fixtures',
    'crypta.profile.lib.test_helpers.fixtures',
]


@pytest.fixture
def date():
    return '2020-10-09'
