import pytest
from decimal import Decimal

from crm.agency_cabinet.common.consts import Currencies
from crm.agency_cabinet.ord.common.consts import ReporterType, ReportStatuses
from crm.agency_cabinet.ord.common.structs import ReportInfo, ReportSettings
from crm.agency_cabinet.ord.proto import request_pb2, reports_pb2
from smb.common.testing_utils import dt


pytestmark = [pytest.mark.asyncio]


async def test_get_reports_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportsInfoOutput(
        result=reports_pb2.ReportsInfoList(
            reports=[reports_pb2.ReportInfo(
                report_id=1,
                status=1,
                reporter_type=0,
                sending_date=None,
                period_from=dt('2020-09-22 10:10:10', as_proto=True),
                clients_count=1,
                campaigns_count=2,
                settings=reports_pb2.ReportSettings(
                    name='other',
                    display_name='Другое',
                    allow_create_ad_distributor_acts=True,
                    allow_create_clients=True,
                    allow_create_campaigns=True,
                    allow_edit_report=True,
                )
            )]
        )
    )

    got = await client.get_reports_info(
        agency_id=1,
        period_to=dt('2022-02-22 10:10:10'),
        period_from=dt('2020-09-22 10:10:10'),
        limit=1,
        offset=0
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_reports_info=reports_pb2.GetReportsInfo(
                agency_id=1,
                period_to=dt('2022-02-22 10:10:10', as_proto=True),
                period_from=dt('2020-09-22 10:10:10', as_proto=True),
                limit=1,
                offset=0
            )
        ),
        response_message_type=reports_pb2.GetReportsInfoOutput,
    )

    assert got == [
        ReportInfo(
            report_id=1,
            status=ReportStatuses.draft,
            reporter_type=ReporterType.partner,
            period_from=dt('2020-09-22 10:10:10'),
            clients_count=1,
            campaigns_count=2,
            settings=ReportSettings(
                name='other',
                display_name='Другое',
                allow_create_ad_distributor_acts=True,
                allow_create_clients=True,
                allow_create_campaigns=True,
                allow_edit_report=True,
            )
        )
    ]


@pytest.mark.skip(reason="Test for wrong method, should be get_detailed_report_info")
async def test_get_detailed_report_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = reports_pb2.GetReportsInfoOutput(
        result=reports_pb2.ReportsInfoList(
            reports=[reports_pb2.ReportInfo(
                report_id=1,
                status=1,
                sending_date=None,
                period_from=dt('2020-09-22 10:10:10', as_proto=True),
                amount='10',
                ad_distributor_id=1,
                ad_distributor_inn='12345',
                ad_distributor_name='yandex',
                ad_distributor_display_name='Яндекс',
                contract_eid='12345',
                currency=0,
                vat=20,
                suggested_amount='10',
                ad_distributors_acts_count=0,
                clients_count=1)
            ]
        )

    )

    got = await client.get_reports_info(agency_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_reports_info=reports_pb2.GetReportsInfo(
                agency_id=1,
            )
        ),
        response_message_type=reports_pb2.GetReportsInfoOutput,
    )

    assert got == [
        ReportInfo(
            report_id=1,
            status=ReportStatuses.draft,
            period_from=dt('2020-09-22 10:10:10'),
            amount=Decimal('10'),
            ad_distributor_id=1,
            ad_distributor_inn='12345',
            ad_distributor_name='yandex',
            ad_distributor_display_name='Яндекс',
            ad_distributor_settings={},
            contract_eid='12345',
            currency=Currencies.rub,
            vat=20,
            suggested_amount=Decimal('10'),
            clients_count=1,
            ad_distributors_acts_count=0,
        )
    ]
