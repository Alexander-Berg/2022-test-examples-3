import pytest
from decimal import Decimal

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, clients_pb2, common_pb2
from crm.agency_cabinet.ord.client.exceptions import NoSuchReportException, UnsuitableAgencyException, \
    UniqueViolationClientException, ForbiddenByReportSettingsException


async def test_create_client_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.CreateClientOutput(
        result=clients_pb2.ClientInfo(
            id=1,
            client_id='client_id',
            login='login',
            suggested_amount='6000.0',
            campaigns_count=0
        )
    )

    got = await client.create_client(agency_id=1, report_id=1, client_id='1')

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            create_client=clients_pb2.CreateClientInput(
                agency_id=1,
                report_id=1,
                client_id='1',
            )
        ),
        response_message_type=clients_pb2.CreateClientOutput,
    )

    assert got == structs.ClientInfo(
        id=1,
        client_id='client_id',
        login='login',
        suggested_amount=Decimal('6000.0'),
        campaigns_count=0,
    )


async def test_unsuitable_agency(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.CreateClientOutput(
        unsuitable_agency=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UnsuitableAgencyException):
        await client.create_client(agency_id=1, report_id=1, client_id='1')


async def test_no_such_report(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.CreateClientOutput(
        no_such_report=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(NoSuchReportException):
        await client.create_client(agency_id=1, report_id=1, client_id='1')


async def test_unique_client(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.CreateClientOutput(
        unique_client_violation=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UniqueViolationClientException):
        await client.create_client(agency_id=1, report_id=1, client_id='1')


async def test_unsuitable_report(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.CreateClientOutput(
        forbidden_by_report_settings=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(ForbiddenByReportSettingsException):
        await client.create_client(agency_id=1, report_id=1, client_id='1')
