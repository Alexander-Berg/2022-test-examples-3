import pytest

from crm.agency_cabinet.certificates.client import Client

pytest_plugins = ["smb.common.rmq.rpc.pytest.plugin"]


@pytest.fixture
def client(rmq_rpc_client):
    return Client(rmq_rpc_client)
