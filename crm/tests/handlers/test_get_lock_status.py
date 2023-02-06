from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, import_data_pb2, common_pb2
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, NoSuchReportException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.LockStatus(
        lock=True
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetLockStatus",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_lock_status=import_data_pb2.GetLockStatusInput(
            agency_id=22,
            report_id=1,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetLockStatusInput(
            agency_id=22,
            report_id=1,
        )
    )


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_lock_status=import_data_pb2.GetLockStatusInput(
            agency_id=22,
            report_id=1,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert import_data_pb2.GetLockStatusOutput.FromString(result) == import_data_pb2.GetLockStatusOutput(
        result=import_data_pb2.LockStatus(
            lock=True
        )
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            import_data_pb2.GetLockStatusOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            NoSuchReportException,
            import_data_pb2.GetLockStatusOutput(no_such_report=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_returns_error(handler, fixture_reports, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetLockStatus",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            get_lock_status=import_data_pb2.GetLockStatusInput(
                agency_id=22,
                report_id=1,
            )
        )

        result = await handler(input_pb.SerializeToString())

        assert import_data_pb2.GetLockStatusOutput.FromString(result) == expected_message
