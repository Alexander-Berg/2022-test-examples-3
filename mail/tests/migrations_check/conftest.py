import pytest

from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.tests.helpers.fixtures import coordinator_context


@pytest.fixture(scope="session")
def mdb():
    with coordinator_context(Mdb) as coord:
        yield coord.components[Mdb]
