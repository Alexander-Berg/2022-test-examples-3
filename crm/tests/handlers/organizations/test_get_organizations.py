import pytest

from unittest.mock import AsyncMock

from crm.agency_cabinet.ord.common import consts, structs
from crm.agency_cabinet.ord.proto import organizations_pb2, request_pb2


@pytest.fixture
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.OrganizationsList(
        size=10,
        organizations=[
            structs.Organization(
                id=42,
                name='Test org',
                type=consts.OrganizationType.ffl,
                inn='1234567890'
            ),
            structs.Organization(
                id=43
            ),
        ]
    )

    mocker.patch(
        'crm.agency_cabinet.ord.server.src.procedures.GetOrganizations',
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    request = request_pb2.RpcRequest(
        get_organizations=organizations_pb2.GetOrganizationsInput(
            partner_id=42,
            limit=20,
            offset=0,
            search_query='inn',
        )
    )

    await handler(request.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetOrganizationsInput(
            partner_id=42,
            limit=20,
            offset=0,
            search_query='inn',
        )
    )


async def test_returns_organizations_list(handler, procedure):
    request = request_pb2.RpcRequest(
        get_organizations=organizations_pb2.GetOrganizationsInput(
            partner_id=42,
        )
    )

    result = await handler(request.SerializeToString())

    assert organizations_pb2.GetOrganizationsOutput.FromString(result) == organizations_pb2.GetOrganizationsOutput(
        result=organizations_pb2.OrganizationsList(
            size=10,
            organizations=[
                organizations_pb2.Organization(
                    id=42,
                    name='Test org',
                    type=0,
                    inn='1234567890'
                ),
                organizations_pb2.Organization(
                    id=43
                )
            ]
        )
    )
