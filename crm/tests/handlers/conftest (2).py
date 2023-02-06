import pytest

from crm.agency_cabinet.certificates.server.lib.handler import Handler


@pytest.fixture
def handler(db):
    return Handler(db)
