import pytest

from crm.agency_cabinet.common.client import BaseClient

pytest_plugins = ['smb.common.rmq.rpc.pytest.plugin']


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


@pytest.fixture
def client(rmq_rpc_client):
    return BaseClient(rmq_rpc_client)
