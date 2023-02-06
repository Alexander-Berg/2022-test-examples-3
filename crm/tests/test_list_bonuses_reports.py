from smb.common.testing_utils import dt

import pytest
from crm.agency_cabinet.client_bonuses.common.structs import (
    ReportInfo,
    ClientType
)
from crm.agency_cabinet.client_bonuses.proto.reports_pb2 import (
    ReportInfo as PbReportInfo,
    BonusesReportsInfoList as PbBonusesReportsInfoList,
    ListBonusesReportsInfoInput as PbListBonusesReportsInfoInput,
    ListBonusesReportsInfoOutput as PbListBonusesReportsInfoOutput,
)

from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import ClientType as PbClientType

from crm.agency_cabinet.client_bonuses.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)
from crm.agency_cabinet.common.consts import ReportsStatuses

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListBonusesReportsInfoOutput(
        reports=PbBonusesReportsInfoList(
            reports=[
                PbReportInfo(id=1,
                             name='Отчет по бонусам 1',
                             status=ReportsStatuses.ready.value,
                             period_from=dt('2021-3-1 00:00:00', as_proto=True),
                             period_to=dt('2021-6-1 00:00:00', as_proto=True),
                             created_at=dt('2021-7-1 00:00:00', as_proto=True),
                             client_type=PbClientType.ACTIVE),
                PbReportInfo(id=2,
                             name='Отчет по бонусам 2',
                             status=ReportsStatuses.in_progress.value,
                             period_from=dt('2021-3-1 00:00:00', as_proto=True),
                             period_to=dt('2021-6-1 00:00:00', as_proto=True),
                             created_at=dt('2021-7-1 00:00:00', as_proto=True),
                             client_type=PbClientType.ACTIVE)])
    )

    await client.list_bonuses_reports(agency_id=22)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=PbRpcRequest(
            list_bonuses_reports=PbListBonusesReportsInfoInput(
                agency_id=22
            )
        ),
        response_message_type=PbListBonusesReportsInfoOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbListBonusesReportsInfoOutput(
        reports=PbBonusesReportsInfoList(
            reports=[
                PbReportInfo(id=1,
                             name='Отчет по бонусам 1',
                             status=ReportsStatuses.ready.value,
                             period_from=dt('2021-3-1 00:00:00', as_proto=True),
                             period_to=dt('2021-6-1 00:00:00', as_proto=True),
                             created_at=dt('2021-7-1 00:00:00', as_proto=True),
                             client_type=PbClientType.ACTIVE),
                PbReportInfo(id=2,
                             name='Отчет по бонусам 2',
                             status=ReportsStatuses.in_progress.value,
                             period_from=dt('2021-3-1 00:00:00', as_proto=True),
                             period_to=dt('2021-6-1 00:00:00', as_proto=True),
                             created_at=dt('2021-7-1 00:00:00', as_proto=True),
                             client_type=PbClientType.ACTIVE)
            ])
    )

    got = await client.list_bonuses_reports(agency_id=22)

    assert got == [
        ReportInfo(id=1,
                   name='Отчет по бонусам 1',
                   status=ReportsStatuses.ready.value,
                   period_from=dt('2021-3-1 00:00:00'),
                   period_to=dt('2021-6-1 00:00:00'),
                   created_at=dt('2021-7-1 00:00:00'),
                   client_type=ClientType.ACTIVE),
        ReportInfo(id=2,
                   name='Отчет по бонусам 2',
                   status=ReportsStatuses.in_progress.value,
                   period_from=dt('2021-3-1 00:00:00'),
                   period_to=dt('2021-6-1 00:00:00'),
                   created_at=dt('2021-7-1 00:00:00'),
                   client_type=ClientType.ACTIVE)
    ]
