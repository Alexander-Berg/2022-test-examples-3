import pytest

pytest_plugins = [
    "crypta.audience.lib.test_helpers.fixtures",
]


@pytest.fixture
def date():
    yield "2022-01-01"
