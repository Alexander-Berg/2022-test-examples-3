import pytest

from crm.agency_cabinet.certificates.client import Client
from crm.agency_cabinet.certificates.proto import common_pb2

pytestmark = [pytest.mark.asyncio]


async def test_ping(client: Client):
    client._client.send_proto_message.return_value = common_pb2.PingOutput(ping="pong")

    result = await client.ping()

    assert result == "pong"
