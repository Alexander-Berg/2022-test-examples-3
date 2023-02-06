import pytest

from crm.agency_cabinet.client_bonuses.server.lib.handler import Handler
from crm.agency_cabinet.common.server.common.config import MdsConfig


@pytest.fixture
def handler(db):
    mds_cfg = MdsConfig.from_environ({
        'MDS_ENDPOINT_URL': 'http://mds.endpoint.url',
        'MDS_ACCESS_KEY_ID': 1,
        'MDS_SECRET_ACCESS_KEY': 1
    })
    return Handler(db, mds_cfg)
