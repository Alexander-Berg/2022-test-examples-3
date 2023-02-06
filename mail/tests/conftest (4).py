import pytest

from mail.devpack.lib.components.all_dumb import FakeRootComponent, Webmail
from mail.devpack.lib.components.unimock import Unimock
from mail.devpack.tests.helpers.fixtures import coordinator_context


class ForTests(FakeRootComponent):
    NAME = 'tested'
    DEPS = [Webmail, Unimock]


@pytest.fixture(scope="session")
def coordinator():
    with coordinator_context(ForTests) as coord:
        yield coord
