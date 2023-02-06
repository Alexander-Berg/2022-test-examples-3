import pytest

from decimal import Decimal
from unittest.mock import AsyncMock

from crm.agency_cabinet.ord.proto import client_rows_pb2, common_pb2, request_pb2, campaigns_pb2, organizations_pb2, \
    contracts_pb2, acts_pb2
from crm.agency_cabinet.ord.common import consts, structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, UnsuitableReportException


@pytest.fixture
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ClientRowsList(
        size=10,
        rows=[
            structs.ClientRow(
                id=42,
                suggested_amount=Decimal(1000.50),
                campaign=structs.Campaign(
                    id=1,
                    campaign_eid='campaign eid',
                    name='campaign name',
                    creative_id='1,2,3,4,5',
                ),
                ad_distributor_organization=structs.Organization(
                    id=41,
                    name='Test org 1',
                    type=consts.OrganizationType.ffl,
                    inn='1234567890',
                ),
                ad_distributor_contract=structs.Contract(
                    id=1,
                    contract_eid='123',
                ),
                ad_distributor_act=structs.Act(
                    act_id=1,
                    act_eid='act1',
                    amount=Decimal(10),
                    is_vat=True,
                ),
            ),
        ]
    )

    mocker.patch(
        'crm.agency_cabinet.ord.server.src.procedures.GetClientRows',
        return_value=mock,
    )

    return mock


async def test_get_client_rows(handler, procedure):
    request_pb = request_pb2.RpcRequest(
        get_client_rows=client_rows_pb2.GetClientRowsInput(
            agency_id=1,
            report_id=2,
            client_id=3,
            limit=4,
            offset=5,
            search_query='12345'
        )
    )

    result = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetClientRowsInput(
            agency_id=1,
            report_id=2,
            client_id=3,
            limit=4,
            offset=5,
            search_query='12345'
        )
    )

    assert client_rows_pb2.GetClientRowsOutput.FromString(result) == client_rows_pb2.GetClientRowsOutput(
        result=client_rows_pb2.ClientRowsList(
            size=10,
            rows=[
                client_rows_pb2.ClientRow(
                    id=42,
                    suggested_amount='1000.50',
                    campaign=campaigns_pb2.Campaign(
                        campaign_eid='campaign eid',
                        name='campaign name',
                        id=1,
                        creative_id='1,2,3,4,5'
                    ),
                    ad_distributor_organization=organizations_pb2.Organization(
                        id=41,
                        name='Test org 1',
                        type=0,
                        inn='1234567890'
                    ),
                    ad_distributor_contract=contracts_pb2.Contract(
                        id=1,
                        contract_eid='123',
                    ),
                    ad_distributor_act=acts_pb2.Act(
                        act_id=1,
                        act_eid='act1',
                        amount='10',
                        is_vat=True,
                    ),
                ),
            ]
        )
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            client_rows_pb2.GetClientRowsOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            UnsuitableReportException,
            client_rows_pb2.GetClientRowsOutput(unsuitable_report=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_get_client_short_info_returns_error(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    mocker.patch(
        'crm.agency_cabinet.ord.server.src.procedures.GetClientRows',
        return_value=mock,
    )

    request_pb = request_pb2.RpcRequest(
        get_client_rows=structs.GetClientRowsInput(
            agency_id=1,
            report_id=2,
            client_id=3
        ).to_proto()
    )
    result = await handler(request_pb.SerializeToString())

    assert client_rows_pb2.GetClientRowsOutput.FromString(result) == expected_message
