import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.grants.client import GrantsClient


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


@pytest.fixture
async def rmq_rpc_client():
    class RmqRpcClientMock:
        send_message = AsyncMock()
        send_proto_message = AsyncMock()

    return RmqRpcClientMock()


@pytest.fixture
def client(rmq_rpc_client):
    return GrantsClient(rmq_rpc_client)
