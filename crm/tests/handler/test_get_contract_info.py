from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.documents.proto.contracts_pb2 import (
    Contract as PbContract,
    ContractStatus as PbContractStatus,
    PaymentType as PbPaymentType,
    Services as PbServices,
    GetContractInfoInput as PbGetContractInfoInput,
    GetContractInfoOutput as PbGetContractInfoOutput,
)
from crm.agency_cabinet.documents.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

from crm.agency_cabinet.common.consts import PaymentType, Services
from crm.agency_cabinet.documents.common.structs.contracts import (
    ContractStatus,
    Contract,
    GetContractInfoInput,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = Contract(
        contract_id=1,
        eid="1234/56",
        status=ContractStatus.valid,
        payment_type=PaymentType.prepayment,
        services=[Services.zen],
        signing_date=dt('2021-3-1 00:00:00'),
        finish_date=dt('2022-3-1 00:00:00'),
    )

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.GetContractInfo",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = RpcRequest(
        get_contract_info=PbGetContractInfoInput(
            agency_id=22,
            contract_id=1,
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        params=GetContractInfoInput(
            agency_id=22,
            contract_id=1,
        )
    )


async def test_returns_serialized_operation_result(handler):
    input_pb = RpcRequest(
        get_contract_info=PbGetContractInfoInput(
            agency_id=22,
            contract_id=1,
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert PbGetContractInfoOutput.FromString(result) == PbGetContractInfoOutput(
        contract=PbContract(
            contract_id=1,
            eid="1234/56",
            status=PbContractStatus.valid,
            payment_type=PbPaymentType.prepayment,
            services=[PbServices.zen],
            signing_date=dt('2021-3-1 00:00:00', as_proto=True),
            finish_date=dt('2022-3-1 00:00:00', as_proto=True),
        )
    )
