import pytest
from crm.agency_cabinet.documents.common.structs import Contract, ContractStatus
from crm.agency_cabinet.documents.proto.contracts_pb2 import (
    Contract as PbContract,
    ContractStatus as PbContractStatus,
    PaymentType as PbPaymentType,
    Services as PbServices,
    GetContractInfoInput as PbGetContractInfoInput,
    GetContractInfoOutput as PbGetContractInfoOutput,
)
from crm.agency_cabinet.documents.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from smb.common.testing_utils import dt
from crm.agency_cabinet.common.consts import PaymentType, Services

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbGetContractInfoOutput(
        contract=PbContract(
            contract_id=1,
            eid="1234/56",
            status=PbContractStatus.valid,
            payment_type=PbPaymentType.prepayment,
            services=[PbServices.direct],
            signing_date=dt('2021-3-1 00:00:00', as_proto=True),
            finish_date=dt('2022-3-1 00:00:00', as_proto=True),
        )
    )

    await client.get_contract_info(agency_id=22, contract_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="documents",
        message=PbRpcRequest(
            get_contract_info=PbGetContractInfoInput(
                agency_id=22,
                contract_id=1,
            )
        ),
        response_message_type=PbGetContractInfoOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbGetContractInfoOutput(
        contract=PbContract(
            contract_id=1,
            eid="1234/56",
            status=PbContractStatus.valid,
            payment_type=PbPaymentType.prepayment,
            services=[PbServices.direct],
            signing_date=dt('2021-3-1 00:00:00', as_proto=True),
            finish_date=dt('2022-3-1 00:00:00', as_proto=True),
        )
    )

    got = await client.get_contract_info(agency_id=22, contract_id=1)

    assert got == Contract(
        contract_id=1,
        eid="1234/56",
        status=ContractStatus.valid,
        payment_type=PaymentType.prepayment,
        services=[Services.direct],
        signing_date=dt('2021-3-1 00:00:00'),
        finish_date=dt('2022-3-1 00:00:00'),
    )
