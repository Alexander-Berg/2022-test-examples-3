from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.documents.common.structs import Agreement, ListAgreementsInput
from crm.agency_cabinet.documents.proto.agreements_pb2 import (
    Agreement as PbAgreement,
    AgreementsList as PbAgreementsList,
    ListAgreementsInput as PbListAgreementsInput,
    ListAgreementsOutput as PbListAgreementsOutput,
)

from crm.agency_cabinet.documents.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Agreement(
            agreement_id=1,
            name='Соглашение 1',
            got_scan=True,
            got_original=True,
            date=dt('2023-4-1 00:00:00'),
        ),
        Agreement(
            agreement_id=2,
            name='Соглашение 2',
            got_scan=True,
            got_original=True,
            date=dt('2023-4-1 00:00:00'),
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.ListAgreements",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        list_agreements=PbListAgreementsInput(
            agency_id=22,
            contract_id=1,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=ListAgreementsInput(
            agency_id=22,
            contract_id=1,
            limit=None,
            offset=None,
            date_to=None,
            date_from=None,
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        list_agreements=PbListAgreementsInput(
            agency_id=22,
            contract_id=1,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbListAgreementsOutput.FromString(result) == PbListAgreementsOutput(
        agreements=PbAgreementsList(
            agreements=[
                PbAgreement(
                    agreement_id=1,
                    name='Соглашение 1',
                    got_scan=True,
                    got_original=True,
                    date=dt('2023-4-1 00:00:00', as_proto=True),
                ),
                PbAgreement(
                    agreement_id=2,
                    name='Соглашение 2',
                    got_scan=True,
                    got_original=True,
                    date=dt('2023-4-1 00:00:00', as_proto=True),
                )
            ]
        )
    )
