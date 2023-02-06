import pytest

from crm.agency_cabinet.ord.client.exceptions import NoSuchReportException, UnsuitableAgencyException, \
    NoSuchClientRowException, UniqueViolationClientRowException
from crm.agency_cabinet.ord.proto import request_pb2, common_pb2, client_rows_pb2


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = client_rows_pb2.EditClientRowOutput(
        result=common_pb2.Empty()
    )

    await client.edit_client_row(agency_id=1,
                                 report_id=1,
                                 client_id=1,
                                 row_id=1,
                                 ad_distributor_act_id=1,
                                 client_contract_id=1,
                                 client_act_id=1,
                                 campaign_eid='eid',
                                 campaign_name='name',
                                 )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            edit_client_row=client_rows_pb2.EditClientRowInput(
                agency_id=1,
                report_id=1,
                client_id=1,
                row_id=1,
                ad_distributor_act_id=1,
                client_contract_id=1,
                client_act_id=1,
                campaign_eid='eid',
                campaign_name='name',
            )
        ),
        response_message_type=client_rows_pb2.EditClientRowOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = client_rows_pb2.EditClientRowOutput(
        result=common_pb2.Empty()
    )

    await client.edit_client_row(agency_id=1, report_id=1, client_id=1, row_id=1)


async def test_unsuitable_agency(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = client_rows_pb2.EditClientRowOutput(
        unsuitable_agency=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UnsuitableAgencyException):
        await client.edit_client_row(agency_id=1, report_id=1, client_id=1, row_id=1)


async def test_no_such_report(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = client_rows_pb2.EditClientRowOutput(
        no_such_report=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(NoSuchReportException):
        await client.edit_client_row(agency_id=1, report_id=1, client_id=1, row_id=1)


async def test_no_such_client_row(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = client_rows_pb2.EditClientRowOutput(
        no_such_client_row=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(NoSuchClientRowException):
        await client.edit_client_row(agency_id=1, report_id=1, client_id=1, row_id=1)


async def test_unique_violation(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = client_rows_pb2.EditClientRowOutput(
        unique_client_row_violation=common_pb2.ErrorMessageResponse()
    )

    with pytest.raises(UniqueViolationClientRowException):
        await client.edit_client_row(agency_id=1, report_id=1, client_id=1, row_id=1)
