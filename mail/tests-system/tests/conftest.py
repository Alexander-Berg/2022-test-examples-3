import pytest

from components.apq_tester import ApqTester
from mail.devpack.tests.helpers.fixtures import coordinator_context


@pytest.fixture(scope="session")
def coordinator():
    with coordinator_context(ApqTester) as coord:
        yield coord
