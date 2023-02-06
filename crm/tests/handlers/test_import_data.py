from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.common.server.common.proto import common_pb2
from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, import_data_pb2, common_pb2 as cm_pb2
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, NoSuchReportException, ImportDataAlreadyRequested


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.TaskInfo(
        task_id=123,
        status=TaskStatuses.requested
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.ImportData",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        import_data=import_data_pb2.ImportDataInput(
            agency_id=22,
            report_id=1,
            filename='test',
            bucket='bucket'
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.ImportDataInput(
            agency_id=22,
            report_id=1,
            filename='test',
            bucket='bucket'
        )
    )


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        import_data=import_data_pb2.ImportDataInput(
            agency_id=22,
            report_id=1,
            filename='test',
            bucket='bucket'
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert import_data_pb2.ImportDataOutput.FromString(result) == import_data_pb2.ImportDataOutput(
        result=import_data_pb2.TaskInfo(
            task_id=123,
            status=common_pb2.TaskStatuses.REQUESTED
        )
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            import_data_pb2.ImportDataOutput(unsuitable_agency=cm_pb2.ErrorMessageResponse(message=''))
        ),
        (
            NoSuchReportException,
            import_data_pb2.ImportDataOutput(no_such_report=cm_pb2.ErrorMessageResponse(message=''))
        ),
        (
            ImportDataAlreadyRequested,
            import_data_pb2.ImportDataOutput(import_already_requested=cm_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_returns_error(handler, fixture_reports, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.ImportData",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            import_data=import_data_pb2.ImportDataInput(
                agency_id=22,
                report_id=1,
                filename='test',
                bucket='bucket'
            )
        )

        result = await handler(input_pb.SerializeToString())

        assert import_data_pb2.ImportDataOutput.FromString(result) == expected_message
