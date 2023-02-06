import pytest

from crm.agency_cabinet.agencies.client import AgenciesClient


pytest_plugins = ['smb.common.rmq.rpc.pytest.plugin']


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


@pytest.fixture
def client(rmq_rpc_client):
    return AgenciesClient(rmq_rpc_client)
