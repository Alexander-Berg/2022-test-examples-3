from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.proto import acts_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableReportException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.Act(
        act_id=1,
        act_eid='eid',
        amount=Decimal('10.0'),
        is_vat=False,
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.AddAct",
        return_value=mock,
    )

    return mock


async def test_add_act(handler):
    request_pb = request_pb2.RpcRequest(
        add_act=acts_pb2.AddActInput(
            agency_id=1,
            report_id=1,
            act_eid='eid',
            amount='10.0',
            is_vat=False,
        )
    )

    result = await handler(request_pb.SerializeToString())

    message = acts_pb2.AddActOutput.FromString(result)

    res = structs.Act.from_proto(message.result)

    assert res == structs.Act(
        act_id=res.act_id,
        act_eid='eid',
        amount=Decimal('10.0'),
        is_vat=False,
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableReportException,
            acts_pb2.AddActOutput(unsuitable_report=common_pb2.ErrorMessageResponse(message=''))
        )
    ]
)
async def test_add_act_bad_report(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.AddAct",
        return_value=mock,
    ):
        request_pb = request_pb2.RpcRequest(
            add_act=acts_pb2.AddActInput(
                agency_id=1,
                report_id=1,
                act_eid='eid',
                amount='10.0',
                is_vat=False,
            )
        )

        result = await handler(request_pb.SerializeToString())

        message = acts_pb2.AddActOutput.FromString(result)

        assert message == expected_message
