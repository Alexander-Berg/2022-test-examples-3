from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, clients_pb2


async def test_get_client_short_info(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.ClientShortInfoOutput(
        result=clients_pb2.ClientShortInfo(
            id=1,
            client_id='1',
            login='login',
            name='name',
        ))

    got = await client.get_client_short_info(agency_id=1, report_id=1, client_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_client_short_info=clients_pb2.ClientShortInfoInput(
                agency_id=1,
                report_id=1,
                client_id=1,
            )
        ),
        response_message_type=clients_pb2.ClientShortInfoOutput,
    )

    assert got == structs.ClientShortInfo(
        id=1,
        client_id='1',
        login='login',
        name='name',
    )
