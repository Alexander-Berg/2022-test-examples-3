from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, reports_pb2, common_pb2
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, NoSuchReportException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = None

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.DeleteReport",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        delete_report=reports_pb2.DeleteReportInput(
            agency_id=22,
            report_id=1,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.DeleteReportRequest(
            agency_id=22,
            report_id=1,
        )
    )


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        delete_report=reports_pb2.DeleteReportInput(
            agency_id=22,
            report_id=1,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert reports_pb2.DeleteReportOutput.FromString(result) == reports_pb2.DeleteReportOutput(
        result=common_pb2.Empty()
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            NoSuchReportException,
            reports_pb2.DeleteReportOutput(no_such_report=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            UnsuitableAgencyException,
            reports_pb2.DeleteReportOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_returns_error(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.DeleteReport",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            delete_report=reports_pb2.DeleteReportInput(
                agency_id=22,
                report_id=1,
            )
        )

        result = await handler(input_pb.SerializeToString())

        assert reports_pb2.DeleteReportOutput.FromString(result) == expected_message
