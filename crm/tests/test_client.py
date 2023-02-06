from crm.agency_cabinet.common.client.tests.proto import common_pb2


async def test_payload(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = common_pb2.TestOutput(payload=common_pb2.TestPayload(value_1=42, value_2='test', value_3=True))

    request = common_pb2.RpcRequest(make_call=common_pb2.Empty())
    result, data = await client._send_message(request, common_pb2.TestOutput)

    assert result == 'payload'
    assert data.value_1 == 42
    assert data.value_2 == 'test'
    assert data.value_3 is True


async def test_payload_with_defaults(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = common_pb2.TestOutput(payload=common_pb2.TestPayload(value_1=0, value_2='', value_3=False))

    request = common_pb2.RpcRequest(make_call=common_pb2.Empty())
    result, data = await client._send_message(request, common_pb2.TestOutput)

    assert result == 'payload'
    assert data.value_1 == 0
    assert data.value_2 == ''
    assert data.value_3 is False


async def test_empty(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = common_pb2.TestOutput(empty=common_pb2.Empty())

    request = common_pb2.RpcRequest(make_call=common_pb2.Empty())

    result, data = await client._send_message(request, common_pb2.TestOutput)
    assert result == 'empty'
