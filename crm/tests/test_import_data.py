import pytest
from crm.agency_cabinet.common.server.common.proto import common_pb2
from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.ord.proto import request_pb2, import_data_pb2
from crm.agency_cabinet.ord.common import structs

pytestmark = [pytest.mark.asyncio]


async def test_import_data_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = import_data_pb2.ImportDataOutput(
        result=import_data_pb2.TaskInfo(
            task_id=123,
            status=common_pb2.TaskStatuses.REQUESTED
        )
    )

    got = await client.import_data(agency_id=1, report_id=1, filename='stub', bucket='stub')

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            import_data=import_data_pb2.ImportDataInput(
                agency_id=1,
                report_id=1,
                filename='stub',
                bucket='stub'
            )
        ),
        response_message_type=import_data_pb2.ImportDataOutput,
    )

    assert got == structs.TaskInfo(
        task_id=123,
        status=TaskStatuses.requested
    )
