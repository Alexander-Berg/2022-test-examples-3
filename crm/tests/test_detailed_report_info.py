import pytest

from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.proto import reports_pb2
from crm.agency_cabinet.common.consts.report import ReportsStatuses
from crm.agency_cabinet.client_bonuses.common import structs
from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import ClientType as PbClientType

pytestmark = [pytest.mark.asyncio]


async def test_returns_data(client, rmq_rpc_client):
    report_info = reports_pb2.ReportInfo(
        id=1,
        name='test',
        created_at=dt('2021-11-11 00:00:00', as_proto=True),
        period_from=dt('2020-11-1 00:00:00', as_proto=True),
        period_to=dt('2020-11-11 00:00:00', as_proto=True),
        status=ReportsStatuses.requested.value,
        client_type=PbClientType.ALL_CLIENTS

    )
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetDetailedReportInfoOutput(
        result=report_info
    )

    got = await client.get_detailed_report_info(agency_id=22, report_id=1)

    assert got == structs.ReportInfo(
        id=1,
        name='test',
        created_at=dt('2021-11-11 00:00:00'),
        period_from=dt('2020-11-1 00:00:00'),
        period_to=dt('2020-11-11 00:00:00'),
        status=ReportsStatuses.requested.value,
        client_type=structs.ClientType.ALL
    )
