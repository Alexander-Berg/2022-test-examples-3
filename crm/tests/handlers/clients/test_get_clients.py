import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.ord.proto import clients_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ClientsInfoList(
        clients=[
            structs.ClientInfo(
                id=1,
                client_id='new client2',
                login='new login',
                suggested_amount=None,
                campaigns_count=0,
                has_valid_ad_distributor=False,
                has_valid_ad_distributor_partner=False,
                has_valid_partner_client=False,
                has_valid_advertiser_contractor=False,
                has_valid_advertiser=False,
            )
        ],
        size=1
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetReportClientsInfo",
        return_value=mock,
    )
    return mock


async def test_report_clients_info(handler, procedure):
    request_pb = request_pb2.RpcRequest(
        get_report_clients_info=clients_pb2.GetReportClientsInfoInput(
            agency_id=1,
            report_id=1,
        )
    )

    result = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetReportClientsInfoInput(
            agency_id=1,
            report_id=1,
            is_valid=None,
            limit=None,
            offset=None,
            search_query=None,
        )
    )

    assert clients_pb2.GetReportClientsInfoOutput.FromString(result) == clients_pb2.GetReportClientsInfoOutput(
        result=clients_pb2.ClientsList(
            clients=[
                clients_pb2.ClientInfo(
                    id=1,
                    client_id='new client2',
                    login='new login',
                    suggested_amount=None,
                    campaigns_count=0,
                    has_valid_ad_distributor=False,
                    has_valid_ad_distributor_partner=False,
                    has_valid_partner_client=False,
                    has_valid_advertiser_contractor=False,
                    has_valid_advertiser=False,
                )
            ],
            size=1
        )
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            clients_pb2.GetReportClientsInfoOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_clients_info_returns_error(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetReportClientsInfo",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            get_report_clients_info=clients_pb2.GetReportClientsInfoInput(
                agency_id=1,
                report_id=1
            )
        )

        result = await handler(input_pb.SerializeToString())

        assert clients_pb2.GetReportClientsInfoOutput.FromString(result) == expected_message
