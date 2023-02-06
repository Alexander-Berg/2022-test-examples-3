from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.documents.proto.contracts_pb2 import (
    Contract as PbContract,
    ContractStatus as PbContractStatus,
    ContractsList as PbContractsList,
    PaymentType as PbPaymentType,
    Services as PbServices,
    ListContractsInput as PbListContractsInput,
    ListContractsOutput as PbListContractsOutput,
)
from crm.agency_cabinet.documents.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

from crm.agency_cabinet.common.consts import PaymentType, Services
from crm.agency_cabinet.documents.common.structs.contracts import (
    ContractStatus,
    Contract,
    ListContractsInput,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Contract(
            contract_id=1,
            eid="1234/56",
            status=ContractStatus.valid,
            payment_type=PaymentType.prepayment,
            services=[Services.zen],
            signing_date=dt('2021-3-1 00:00:00'),
            finish_date=dt('2022-3-1 00:00:00'),
        ),
        Contract(
            contract_id=2,
            eid="1234/89",
            status=ContractStatus.not_signed,
            payment_type=PaymentType.postpayment,
            services=[Services.zen],
            signing_date=None,
            finish_date=dt('2022-3-1 00:00:00'),
        ),
        Contract(
            contract_id=3,
            eid="1234/12",
            status=ContractStatus.not_signed,
            payment_type=None,
            services=[],
            signing_date=None,
            finish_date=dt('2022-3-1 00:00:00'),
        ),
    ]

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.ListContracts",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        list_contracts=PbListContractsInput(
            agency_id=22
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=ListContractsInput(
            agency_id=22
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        list_contracts=PbListContractsInput(
            agency_id=22
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbListContractsOutput.FromString(result) == PbListContractsOutput(
        contracts=PbContractsList(
            contracts=[
                PbContract(
                    contract_id=1,
                    eid="1234/56",
                    status=PbContractStatus.valid,
                    payment_type=PbPaymentType.prepayment,
                    services=[PbServices.zen],
                    signing_date=dt('2021-3-1 00:00:00', as_proto=True),
                    finish_date=dt('2022-3-1 00:00:00', as_proto=True),
                ),
                PbContract(
                    contract_id=2,
                    eid="1234/89",
                    status=PbContractStatus.not_signed,
                    payment_type=PbPaymentType.postpayment,
                    services=[PbServices.zen],
                    signing_date=None,
                    finish_date=dt('2022-3-1 00:00:00', as_proto=True),
                ),
                PbContract(
                    contract_id=3,
                    eid="1234/12",
                    status=PbContractStatus.not_signed,
                    services=[],
                    signing_date=None,
                    finish_date=dt('2022-3-1 00:00:00', as_proto=True),
                ),
            ]
        )
    )
