import pytest

from smb.common.testing_utils import dt

from crm.agency_cabinet.ord.proto import request_pb2, reports_pb2
from crm.agency_cabinet.ord.common.consts import ReporterType


pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.CreateReportOutput(
        result=reports_pb2.ReportInfo(
            report_id=1,
            period_from=dt('2020-09-22 10:10:10', as_proto=True),
            status=1,
            reporter_type=0,
            clients_count=0,
            campaigns_count=0,
            settings=reports_pb2.ReportSettings(
                name='other',
                display_name='Другое',
                allow_create_ad_distributor_acts=True,
                allow_create_clients=True,
                allow_create_campaigns=True,
                allow_edit_report=True,
            ),
            sending_date=dt('2020-09-22 10:10:10', as_proto=True)
        )
    )

    await client.create_report(agency_id=1,
                               period_from=dt('2020-09-22 10:10:10'),
                               reporter_type=ReporterType.partner,)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            create_report=reports_pb2.CreateReportInput(
                agency_id=1,
                period_from=dt('2020-09-22 10:10:10', as_proto=True),
                reporter_type=0,
            )
        ),
        response_message_type=reports_pb2.CreateReportOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.CreateReportOutput(
        result=reports_pb2.ReportInfo(
            report_id=1,
            period_from=dt('2020-09-22 10:10:10', as_proto=True),
            status=1,
            reporter_type=0,
            clients_count=0,
            campaigns_count=0,
            settings=reports_pb2.ReportSettings(
                name='other',
                display_name='Другое',
                allow_create_ad_distributor_acts=True,
                allow_create_clients=True,
                allow_create_campaigns=True,
                allow_edit_report=True,
            ),
            sending_date=dt('2020-09-22 10:10:10', as_proto=True)
        )
    )

    await client.create_report(
        agency_id=1,
        period_from=dt('2020-09-22 10:10:10'),
        reporter_type=ReporterType.partner)
