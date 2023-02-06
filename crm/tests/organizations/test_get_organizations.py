import pytest

from crm.agency_cabinet.ord.common import consts, structs
from crm.agency_cabinet.ord.proto import request_pb2, organizations_pb2


async def test_sends_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = organizations_pb2.GetOrganizationsOutput(
        result=organizations_pb2.OrganizationsList(
            size=0,
            organizations=[]
        )
    )

    await client.get_organizations(partner_id=42, limit=10, offset=20, search_query='search')

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_organizations=organizations_pb2.GetOrganizationsInput(
                partner_id=42,
                limit=10,
                offset=20,
                search_query='search'
            )
        ),
        response_message_type=organizations_pb2.GetOrganizationsOutput,
    )


@pytest.mark.parametrize(
    ('organizations_list_proto', 'expected_result'),
    [
        (
            organizations_pb2.OrganizationsList(size=0, organizations=[]),
            structs.OrganizationsList(size=0, organizations=[])
        ),
        (
            organizations_pb2.OrganizationsList(size=5, organizations=[
                organizations_pb2.Organization(
                    id=42,
                    name='Test org',
                    type=0,
                    inn='1234567890'
                ),
                organizations_pb2.Organization(
                    id=43
                )
            ]),
            structs.OrganizationsList(size=5, organizations=[
                structs.Organization(
                    id=42,
                    name='Test org',
                    type=consts.OrganizationType.ffl,
                    inn='1234567890'
                ),
                structs.Organization(
                    id=43
                ),
            ])
        )
    ]
)
async def test_returns_organizations(client, rmq_rpc_client, organizations_list_proto, expected_result):
    rmq_rpc_client.send_proto_message.return_value = organizations_pb2.GetOrganizationsOutput(
        result=organizations_list_proto
    )

    result = await client.get_organizations(partner_id=42)

    assert result == expected_result
