import pytest

from crm.agency_cabinet.client_bonuses.proto import reports_pb2


pytestmark = [pytest.mark.asyncio]


async def test_returns_data(client, rmq_rpc_client):
    report_url = 'test.url'
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportUrlOutput(
        result=reports_pb2.ReportUrl(url=report_url)
    )

    got = await client.get_report_url(agency_id=22, report_id=1)

    assert got == report_url
