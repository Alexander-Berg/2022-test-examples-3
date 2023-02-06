from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.proto import acts_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableReportException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ActList(
        size=2,
        acts=[
            structs.Act(
                act_id=1,
                act_eid='act5',
                amount=Decimal('5'),
                is_vat=True,
            ),
            structs.Act(
                act_id=1,
                act_eid='act4',
                amount=Decimal('3'),
                is_vat=False,
            ),
        ]
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetActs",
        return_value=mock,
    )

    return mock


async def test_ord_get_acts(handler):
    request_pb = request_pb2.RpcRequest(
        get_acts=acts_pb2.GetActsInput(
            limit=5,
            offset=0,
            report_id=1,
            agency_id=1
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = acts_pb2.GetActsOutput.FromString(data)

    res = structs.ActList.from_proto(message.result)

    assert res == structs.ActList(
        size=2,
        acts=[
            structs.Act(
                act_id=1,
                act_eid='act5',
                amount=Decimal('5'),
                is_vat=True,
            ),
            structs.Act(
                act_id=1,
                act_eid='act4',
                amount=Decimal('3'),
                is_vat=False,
            ),
        ]
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableReportException,
            acts_pb2.GetActsOutput(unsuitable_report=common_pb2.ErrorMessageResponse(message=''))
        )
    ]
)
async def test_ord_get_acts_bad_report(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetActs",
        return_value=mock,
    ):
        request_pb = request_pb2.RpcRequest(
        get_acts=acts_pb2.GetActsInput(
            limit=5,
            offset=0,
            report_id=1,
            agency_id=1,
        )
    )

        result = await handler(request_pb.SerializeToString())

        message = acts_pb2.GetActsOutput.FromString(result)

        assert message == expected_message


async def test_calls_procedure_get_acts(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_acts=acts_pb2.GetActsInput(
            limit=5,
            offset=0,
            report_id=1,
            agency_id=1
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(request=structs.GetActsInput(
        offset=0,
        limit=5,
        report_id=1,
        agency_id=1
    ))
