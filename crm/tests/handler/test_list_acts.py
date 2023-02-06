from decimal import Decimal
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.documents.common.structs import Act, ListActsInput
from crm.agency_cabinet.documents.proto.acts_pb2 import (
    Act as PbAct,
    ActsList as PbActsList,
    ListActsInput as PbListActsInput,
    ListActsOutput as PbListActsOutput,
)

from crm.agency_cabinet.documents.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Act(
            act_id=1,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('66.6'),
            date=dt('2022-3-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='test_invoice_eid',
        ),
        Act(
            act_id=2,
            invoice_id=1,
            contract_id=1,
            currency='RUR',
            amount=Decimal('77.7'),
            date=dt('2022-8-1 00:00:00'),
            eid='test_eid',
            contract_eid='test_contract_eid',
            invoice_eid='test_invoice_eid',
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.ListActs",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        list_acts=PbListActsInput(
            agency_id=22,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=ListActsInput(
            agency_id=22,
            invoice_id=None,
            contract_id=None,
            limit=None,
            offset=None,
            date_to=None,
            date_from=None,
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        list_acts=PbListActsInput(
            agency_id=22,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbListActsOutput.FromString(result) == PbListActsOutput(
        acts=PbActsList(
            acts=[
                PbAct(
                    act_id=1,
                    invoice_id=1,
                    contract_id=1,
                    currency='RUR',
                    amount='66.6',
                    date=dt('2022-3-1 00:00:00', as_proto=True),
                    eid='test_eid',
                    contract_eid='test_contract_eid',
                    invoice_eid='test_invoice_eid',
                ),
                PbAct(
                    act_id=2,
                    invoice_id=1,
                    contract_id=1,
                    currency='RUR',
                    amount='77.7',
                    date=dt('2022-8-1 00:00:00', as_proto=True),
                    eid='test_eid',
                    contract_eid='test_contract_eid',
                    invoice_eid='test_invoice_eid',
                )
            ]
        )
    )
