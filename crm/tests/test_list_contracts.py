import pytest
from crm.agency_cabinet.documents.common.structs import Contract, ContractStatus
from crm.agency_cabinet.documents.proto.contracts_pb2 import (
    Contract as PbContract,
    ContractStatus as PbContractStatus,
    ContractsList as PbContractsList,
    PaymentType as PbPaymentType,
    Services as PbServices,
    ListContractsInput as PbListContractsInput,
    ListContractsOutput as PbListContractsOutput,
)
from crm.agency_cabinet.documents.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from smb.common.testing_utils import dt
from crm.agency_cabinet.common.consts import PaymentType, Services

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListContractsOutput(
        contracts=PbContractsList(
            contracts=[
                PbContract(
                    contract_id=1,
                    eid="1234/56",
                    status=PbContractStatus.valid,
                    payment_type=PbPaymentType.prepayment,
                    services=[PbServices.direct],
                    signing_date=dt('2021-3-1 00:00:00', as_proto=True),
                    finish_date=dt('2022-3-1 00:00:00', as_proto=True),
                ),
                PbContract(
                    contract_id=2,
                    eid="1234/89",
                    status=PbContractStatus.valid,
                    payment_type=PbPaymentType.prepayment,
                    services=[PbServices.direct],
                    signing_date=dt('2021-5-1 00:00:00', as_proto=True),
                    finish_date=dt('2022-5-1 00:00:00', as_proto=True),
                )
            ])
    )

    await client.list_contracts(agency_id=22)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="documents",
        message=PbRpcRequest(
            list_contracts=PbListContractsInput(
                agency_id=22
            )
        ),
        response_message_type=PbListContractsOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListContractsOutput(
        contracts=PbContractsList(
            contracts=[
                PbContract(
                    contract_id=1,
                    eid="1234/56",
                    status=PbContractStatus.valid,
                    payment_type=PbPaymentType.prepayment,
                    services=[PbServices.direct],
                    signing_date=dt('2021-3-1 00:00:00', as_proto=True),
                    finish_date=dt('2022-3-1 00:00:00', as_proto=True),
                ),
                PbContract(
                    contract_id=2,
                    eid="1234/89",
                    status=PbContractStatus.valid,
                    payment_type=PbPaymentType.prepayment,
                    services=[PbServices.direct],
                    signing_date=dt('2021-5-1 00:00:00', as_proto=True),
                    finish_date=dt('2022-5-1 00:00:00', as_proto=True),
                )
            ])
    )

    got = await client.list_contracts(agency_id=22)

    assert got == [
        Contract(
            contract_id=1,
            eid="1234/56",
            status=ContractStatus.valid,
            payment_type=PaymentType.prepayment,
            services=[Services.direct],
            signing_date=dt('2021-3-1 00:00:00'),
            finish_date=dt('2022-3-1 00:00:00'),
        ),
        Contract(
            contract_id=2,
            eid="1234/89",
            status=ContractStatus.valid,
            payment_type=PaymentType.prepayment,
            services=[Services.direct],
            signing_date=dt('2021-5-1 00:00:00'),
            finish_date=dt('2022-5-1 00:00:00'),
        )
    ]
