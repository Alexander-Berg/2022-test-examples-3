from crm.agency_cabinet.agencies.common import QUEUE
from crm.agency_cabinet.agencies.proto import request_pb2, common_pb2
from crm.agency_cabinet.agencies.client import AgenciesClient
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_ping(client: AgenciesClient, rmq_rpc_client: RmqRpcClient):
    rmq_rpc_client.send_proto_message.return_value = common_pb2.PingOutput(ping='pong')

    response = await client.ping()

    assert response == 'pong'

    request = request_pb2.RpcRequest(ping=common_pb2.Empty())

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=common_pb2.PingOutput
    )
