import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.ord.proto import clients_pb2, request_pb2, common_pb2
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, NoSuchReportException, \
    ForbiddenByReportSettingsException, UniqueViolationClientException


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ClientInfo(
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

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.CreateClient",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        create_client=clients_pb2.CreateClientInput(
            agency_id=22,
            report_id=1,
            client_id='new client2',
            login='new login',
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.CreateClientInput(
            agency_id=22,
            report_id=1,
            client_id='new client2',
            login='new login',
        )
    )


async def test_create_client_all_data(
    handler, procedure
):
    request_pb = request_pb2.RpcRequest(
        create_client=clients_pb2.CreateClientInput(
            agency_id=22,
            report_id=1,
            client_id='new client2',
            login='new login',
        )
    )

    result = await handler(request_pb.SerializeToString())

    assert clients_pb2.CreateClientOutput.FromString(result) == clients_pb2.CreateClientOutput(
        result=clients_pb2.ClientInfo(
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
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            clients_pb2.CreateClientOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            NoSuchReportException,
            clients_pb2.CreateClientOutput(no_such_report=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            ForbiddenByReportSettingsException,
            clients_pb2.CreateClientOutput(forbidden_by_report_settings=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            UniqueViolationClientException,
            clients_pb2.CreateClientOutput(unique_client_violation=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_create_client_returns_error(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    with mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.CreateClient",
        return_value=mock,
    ):
        input_pb = request_pb2.RpcRequest(
            create_client=clients_pb2.CreateClientInput(
                agency_id=1,
                report_id=1,
                client_id='new client',
            )
        )

        result = await handler(input_pb.SerializeToString())
        assert clients_pb2.CreateClientOutput.FromString(result) == expected_message
