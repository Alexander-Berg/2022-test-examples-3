import pytest
from crm.agency_cabinet.ord.proto import request_pb2, import_data_pb2
from crm.agency_cabinet.ord.common import structs

pytestmark = [pytest.mark.asyncio]


async def test_get_lock_status_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = import_data_pb2.GetLockStatusOutput(
        result=import_data_pb2.LockStatus(
            lock=True
        )
    )

    got = await client.get_lock_status(agency_id=1, report_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_lock_status=import_data_pb2.GetLockStatusInput(
                agency_id=1,
                report_id=1,
            )
        ),
        response_message_type=import_data_pb2.GetLockStatusOutput,
    )

    assert got == structs.LockStatus(
        lock=True
    )
