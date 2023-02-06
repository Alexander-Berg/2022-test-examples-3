import pytest
from decimal import Decimal

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.proto import request_pb2, clients_pb2

pytestmark = [pytest.mark.asyncio]


async def test_get_reports_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = clients_pb2.GetReportClientsInfoOutput(
        result=clients_pb2.ClientsList(
            clients=[
                clients_pb2.ClientInfo(
                    id=1,
                    client_id='client_id',
                    login='login',
                    suggested_amount='6000.0',
                    has_valid_partner_client=False,
                    has_valid_advertiser=True,
                    has_valid_ad_distributor=True,
                    has_valid_advertiser_contractor=True,
                    has_valid_ad_distributor_partner=False,
                    campaigns_count=2,
                )
            ]
        )
    )

    got = await client.get_report_clients_info(agency_id=1, report_id=1, is_valid=True)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_report_clients_info=clients_pb2.GetReportClientsInfoInput(
                agency_id=1,
                report_id=1,
                is_valid=True,
            )
        ),
        response_message_type=clients_pb2.GetReportClientsInfoOutput,
    )

    assert got == [
        structs.ClientInfo(
            id=1,
            client_id='client_id',
            login='login',
            suggested_amount=Decimal('6000.0'),
            has_valid_partner_client=False,
            has_valid_advertiser=True,
            has_valid_ad_distributor=True,
            has_valid_advertiser_contractor=True,
            has_valid_ad_distributor_partner=False,
            campaigns_count=2,
        )
    ]
