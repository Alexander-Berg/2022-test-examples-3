from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.proto import acts_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import NoSuchActException, UnsuitableReportException


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
        "crm.agency_cabinet.ord.server.src.procedures.EditAct",
        return_value=mock,
    )

    return mock


async def test_edit_act(handler):
    request_pb = request_pb2.RpcRequest(
        edit_act=acts_pb2.EditActInput(
            agency_id=1,
            report_id=1,
            act_id=1,
            act_eid='eid',
            amount='10.0',
            is_vat=False,
        )
    )

    result = await handler(request_pb.SerializeToString())

    message = acts_pb2.EditActOutput.FromString(result)

    assert message.result == common_pb2.Empty()


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableReportException,
            acts_pb2.EditActOutput(unsuitable_report=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            NoSuchActException,
            acts_pb2.EditActOutput(no_such_act=common_pb2.ErrorMessageResponse(message=''))
        )
    ]
)
async def test_edit_act_bad_report(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.EditAct",
        return_value=mock,
    ):
        request_pb = request_pb2.RpcRequest(
            edit_act=acts_pb2.EditActInput(
                agency_id=1,
                report_id=1,
                act_id=1,
                act_eid='eid',
                amount='10.0',
                is_vat=False,
            )
        )

        result = await handler(request_pb.SerializeToString())

        message = acts_pb2.EditActOutput.FromString(result)

        assert message == expected_message
