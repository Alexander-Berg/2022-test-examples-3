import pytest

from crm.agency_cabinet.certificates.server.lib.db.engine import DB
from smb.common.multiruntime import io

io.setup_filesystem("crm/agency_cabinet/certificates/server")


pytest_plugins = [
    "smb.common.pgswim.pytest.plugin",
    "crm.agency_cabinet.certificates.server.tests.factory",
]


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB
