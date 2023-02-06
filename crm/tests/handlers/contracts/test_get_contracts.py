import pytest
from decimal import Decimal
from datetime import datetime, timezone
from unittest.mock import AsyncMock

from crm.agency_cabinet.ord.proto import contracts_pb2, request_pb2
from crm.agency_cabinet.ord.common import structs


@pytest.fixture()
def contracts():
    return structs.ContractsList(
        size=1,
        contracts=[
            structs.Contract(
                id=1,
                contract_eid='123',
                is_reg_report=True,
                type='contract',
                action_type='other',
                subject_type='other',
                date=datetime.now(tz=timezone.utc),
                amount=Decimal(100),
                is_vat=True,
                client_organization=structs.Organization(
                    id=1,
                    type='ffl',
                    name='test',
                    inn='123456',
                    is_rr=False,
                    is_ors=False,
                    mobile_phone='5353535',
                    epay_number='123',
                    reg_number='321',
                    alter_inn='654321',
                    oksm_number='1',
                    rs_url='test'
                ),
                contractor_organization=structs.Organization(
                    id=2,
                    type='ffl',
                    name='test2',
                    inn='654321',
                    is_rr=False,
                    is_ors=False,
                    mobile_phone='3535353',
                    epay_number='321',
                    reg_number='123',
                    alter_inn='123456',
                    oksm_number='2',
                    rs_url='test2'
                ),
            )
        ]
    )


@pytest.fixture(autouse=True)
def procedure(mocker, contracts):
    mock = AsyncMock()
    mock.return_value = contracts

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetContracts",
        return_value=mock,
    )

    return mock


async def test_ord_get_contracts(
        handler,
        contracts
):
    request_pb = request_pb2.RpcRequest(
        get_contracts=contracts_pb2.GetContractsInput(
            limit=5,
            offset=0,
            agency_id=1
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = contracts_pb2.GetContractsOutput.FromString(data)

    res = structs.ContractsList.from_proto(message.result)

    assert res == contracts
