import pytest

from crm.agency_cabinet.ord.common.structs import ContractsList, Contract, Organization
from crm.agency_cabinet.ord.proto import request_pb2, contracts_pb2, organizations_pb2

pytestmark = [pytest.mark.asyncio]


async def test_ord_get_contracts(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = contracts_pb2.GetContractsOutput(
        result=contracts_pb2.ContractsList(
            contracts=[
                contracts_pb2.Contract(
                    id=1,
                    contract_eid='123',
                    client_organization=organizations_pb2.Organization(
                        id=1,
                    )
                ),
                contracts_pb2.Contract(
                    id=2,
                    contract_eid='124',
                    contractor_organization=organizations_pb2.Organization(
                        id=2,
                    )
                )
            ],
            size=2
        ))

    got = await client.get_contracts(
        search_query='search_query',
        limit=2,
        offset=0,
        agency_id=1,
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_contracts=contracts_pb2.GetContractsInput(
                search_query='search_query',
                limit=2,
                offset=0,
                agency_id=1,
            )
        ),
        response_message_type=contracts_pb2.GetContractsOutput,
    )

    assert got == ContractsList(
        size=2,
        contracts=[
            Contract(
                id=1,
                contract_eid='123',
                client_organization=Organization(id=1)
            ),
            Contract(
                id=2,
                contract_eid='124',
                contractor_organization=Organization(id=2)
            )
        ]
    )
