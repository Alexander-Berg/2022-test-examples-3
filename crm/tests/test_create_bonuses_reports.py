import pytest

from crm.agency_cabinet.client_bonuses.common.structs import (
    ReportInfo,
    ClientType
)

from crm.agency_cabinet.client_bonuses.proto.reports_pb2 import (
    ReportInfo as PbReportInfo,
    CreateReportInput as PbCreateReportInput,
    CreateReportOutput as PbCreateReportOutput,
)

from crm.agency_cabinet.client_bonuses.proto.request_pb2 import (
    RpcRequest as PbRpcRequest,
)

from crm.agency_cabinet.client_bonuses.proto.bonuses_pb2 import ClientType as PbClientType

from smb.common.testing_utils import dt
from crm.agency_cabinet.common.consts.report import ReportsStatuses

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbCreateReportOutput(
        result=PbReportInfo(
            id=1,
            name='test',
            created_at=dt('2021-11-11 00:00:00', as_proto=True),
            period_from=dt('2020-11-1 00:00:00', as_proto=True),
            period_to=dt('2020-11-11 00:00:00', as_proto=True),
            status=ReportsStatuses.requested.value,
            client_type=PbClientType.ALL_CLIENTS
        )
    )

    await client.create_report(
        agency_id=22,
        name='test',
        period_from=dt('2020-11-1 00:00:00'),
        period_to=dt('2020-11-11 00:00:00'),
        client_type=ClientType.ALL
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name="client-bonuses",
        message=PbRpcRequest(
            create_report=PbCreateReportInput(
                agency_id=22,
                name='test',
                period_from=dt('2020-11-1 00:00:00', as_proto=True),
                period_to=dt('2020-11-11 00:00:00', as_proto=True),
                client_type=PbClientType.ALL_CLIENTS
            )
        ),
        response_message_type=PbCreateReportOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = PbCreateReportOutput(
        result=PbReportInfo(
            id=1,
            name='test',
            created_at=dt('2021-11-11 00:00:00', as_proto=True),
            period_from=dt('2020-11-1 00:00:00', as_proto=True),
            period_to=dt('2020-11-11 00:00:00', as_proto=True),
            status=ReportsStatuses.requested.value,
            client_type=PbClientType.ALL_CLIENTS
        )
    )

    got = await client.create_report(
        agency_id=22,
        name='test',
        period_from=dt('2020-11-1 00:00:00'),
        period_to=dt('2020-11-11 00:00:00'),
        client_type=ClientType.ALL
    )

    assert got == ReportInfo(
        id=1,
        name='test',
        created_at=dt('2021-11-11 00:00:00'),
        period_from=dt('2020-11-1 00:00:00'),
        period_to=dt('2020-11-11 00:00:00'),
        status=ReportsStatuses.requested.value,
        client_type=ClientType.ALL

    )
