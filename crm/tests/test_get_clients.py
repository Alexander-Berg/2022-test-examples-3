from crm.agency_cabinet.agencies.common import QUEUE
from crm.agency_cabinet.agencies.proto import request_pb2, clients_pb2
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.client import AgenciesClient
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_get_clients(client: AgenciesClient, rmq_rpc_client: RmqRpcClient):
    client_pb = clients_pb2.ClientInfo(
        id=1,
        name='тестовое имя',
        login='testlogin'
    )
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.GetClientsInfoOutput(
        result=clients_pb2.ClientsInfoList(
            clients=[
                client_pb
            ]
        )
    )

    response = await client.get_clients_info(123, limit=1)

    assert response == [structs.ClientInfo.from_proto(client_pb)]

    request = request_pb2.RpcRequest(get_clients_info=clients_pb2.GetClientsInfo(
        agency_id=123,
        limit=1
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=clients_pb2.GetClientsInfoOutput
    )
