from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.client_bonuses.common.structs import (
    DeleteReportInput,
    DeleteReportOutput
)
from crm.agency_cabinet.client_bonuses.proto.common_pb2 import Empty as PbEmpty
from crm.agency_cabinet.client_bonuses.proto.reports_pb2 import (
    DeleteResponse as PbDeleteResponse,
    DeleteReportInput as PbDeleteReportInput,
    DeleteReportOutput as PbDeleteReportOutput,
)
from crm.agency_cabinet.client_bonuses.proto.request_pb2 import RpcRequest
from crm.agency_cabinet.client_bonuses.server.lib.exceptions import NoSuchReportException, UnsuitableAgencyException

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = DeleteReportOutput(is_deleted=True)

    mocker.patch(
        "crm.agency_cabinet.client_bonuses.server.lib.handler.DeleteReport",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        delete_report=PbDeleteReportInput(
            agency_id=22,
            report_id=1
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=DeleteReportInput(
            agency_id=22,
            report_id=1
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        delete_report=PbDeleteReportInput(
            agency_id=22,
            report_id=1
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbDeleteReportOutput.FromString(result) == PbDeleteReportOutput(
        result=PbDeleteResponse(is_deleted=True)
    )


async def test_returns_error_if_report_not_found(handler, procedure):
    procedure.side_effect = NoSuchReportException()

    input_pb = RpcRequest(
        delete_report=PbDeleteReportInput(
            agency_id=22,
            report_id=1
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbDeleteReportOutput.FromString(result) == PbDeleteReportOutput(
        no_such_report=PbEmpty()
    )


async def test_returns_error_if_unsuitable_agency(handler, procedure):
    procedure.side_effect = UnsuitableAgencyException()

    input_pb = RpcRequest(
        delete_report=PbDeleteReportInput(
            agency_id=22,
            report_id=1
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbDeleteReportOutput.FromString(result) == PbDeleteReportOutput(
        unsuitable_agency=PbEmpty()
    )
