from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, common_pb2, client_rows_pb2
from crm.agency_cabinet.ord.common.exceptions import NoSuchReportException, UnsuitableReportException, \
    UnsuitableAgencyException, NoSuchClientRowException, UniqueViolationClientRowException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = None

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.EditClientRow",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        edit_client_row=client_rows_pb2.EditClientRowInput(
            agency_id=22,
            report_id=1,
            client_id=1,
            row_id=1,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.EditClientRowInput(
            agency_id=22,
            report_id=1,
            client_id=1,
            row_id=1,
        )
    )


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        edit_client_row=client_rows_pb2.EditClientRowInput(
            agency_id=22,
            report_id=1,
            client_id=1,
            row_id=1
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert client_rows_pb2.EditClientRowOutput.FromString(result) == client_rows_pb2.EditClientRowOutput(
        result=common_pb2.Empty()
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            client_rows_pb2.EditClientRowOutput(unsuitable_agency=common_pb2.ErrorMessageResponse())
        ),
(
            UnsuitableReportException,
            client_rows_pb2.EditClientRowOutput(unsuitable_report=common_pb2.ErrorMessageResponse())
        ),
        (
            NoSuchReportException,
            client_rows_pb2.EditClientRowOutput(no_such_report=common_pb2.ErrorMessageResponse())
        ),
        (
            NoSuchClientRowException,
            client_rows_pb2.EditClientRowOutput(no_such_client_row=common_pb2.ErrorMessageResponse())
        ),
        (
            UniqueViolationClientRowException,
            client_rows_pb2.EditClientRowOutput(unique_client_row_violation=common_pb2.ErrorMessageResponse())
        )
    ]
)
async def test_returns_error(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.EditClientRow",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            edit_client_row=client_rows_pb2.EditClientRowInput(
                agency_id=22,
                report_id=1,
                client_id=1,
                row_id=1
            )
        )

        result = await handler(input_pb.SerializeToString())

        assert client_rows_pb2.EditClientRowOutput.FromString(result) == expected_message
