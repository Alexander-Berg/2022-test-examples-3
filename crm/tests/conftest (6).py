import pytest
from smb.common.multiruntime import io

from crm.agency_cabinet.client_bonuses.server.lib.db.engine import DB

io.setup_filesystem("crm/agency_cabinet/client_bonuses/server")


pytest_plugins = [
    "smb.common.pgswim.pytest.plugin",
    "crm.agency_cabinet.client_bonuses.server.tests.factory",
]


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB
