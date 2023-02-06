import pytest

pytest_plugins = [
    'crypta.profile.lib.test_helpers.fixtures',
]


@pytest.fixture
def date():
    return '2020-03-29'
