import datetime
import pytest
import typing
from decimal import Decimal
from smb.common.testing_utils import dt

from crm.agency_cabinet.common.consts import Currencies
from crm.agency_cabinet.ord.proto import reports_pb2, request_pb2
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs


@pytest.mark.skip(reason="Real models shouldn't used here")
async def test_ord_get_reports_info(
    handler,
    fixture_reports: typing.List[models.Report],
):
    rpc_request_kwargs = {
        'status': structs.report_status_converter.reversed(structs.ReportStatuses.draft),
        'search_query': None,
        'period_from': dt('2020-01-01 00:00:00', as_proto=True),
        'period_to': dt('2022-01-01 00:00:00', as_proto=True),
        'sort': [
            structs.ReportSort(
                column=structs.ReportColumns.sending_date,
                type=structs.SortTypes.asc
            ).to_proto(),
            structs.ReportSort(
                column=structs.ReportColumns.clients_count,
                type=structs.SortTypes.asc
            ).to_proto(),
            structs.ReportSort(
                column=structs.ReportColumns.status,
                type=structs.SortTypes.desc
            ).to_proto()
        ]

    }
    expected = structs.GetReportsInfoResponse(
        reports=[structs.ReportInfo(
            report_id=0,
            status=structs.ReportStatuses.draft, period_from=dt('2021-02-01 00:00:00'),
            ad_distributor_id=fixture_reports[0].ad_distributor_id,
            ad_distributor_inn='yandex inn',
            ad_distributor_name='yandex',
            ad_distributor_display_name='Яндекс',
            ad_distributor_settings={},
            contract_eid='12345',
            currency=Currencies.rub,
            vat=20,
            agency_inn=fixture_reports[0].agency_inn,
            ad_distributors_acts_count=0,
            amount=Decimal('20'), suggested_amount=Decimal('10'), clients_count=2),
        ]
    )
    indexes = [1]
    request_pb = request_pb2.RpcRequest(
        get_reports_info=reports_pb2.GetReportsInfo(
            agency_id=fixture_reports[0].agency_id,
            **rpc_request_kwargs
        )
    )

    data = await handler(request_pb.SerializeToString())
    message = reports_pb2.GetReportsInfoOutput.FromString(data)
    res = structs.GetReportsInfoResponse.from_proto(message.result)

    for idx, report in zip(indexes, expected.reports):
        report.report_id = fixture_reports[idx].id
    assert res == expected


@pytest.mark.skip(reason="Real models shouldn't used here")
async def test_ord_get_reports_info2(
    handler,
    fixture_reports: typing.List[models.Report],
):
    rpc_request_kwargs = {
        'status': structs.report_status_converter.reversed(structs.ReportStatuses.sent),
        'search_query': None,
        'period_from': dt('2022-01-01 00:00:00', as_proto=True),
        'period_to': dt('2022-02-01 00:00:00', as_proto=True),
        'sort': [
            structs.ReportSort(
                column=structs.ReportColumns.sending_date,
                type=structs.SortTypes.asc
            ).to_proto(),
            structs.ReportSort(
                column=structs.ReportColumns.clients_count,
                type=structs.SortTypes.asc
            ).to_proto(),
            structs.ReportSort(
                column=structs.ReportColumns.status,
                type=structs.SortTypes.desc
            ).to_proto()
        ]

    }
    expected = structs.GetReportsInfoResponse(
        reports=[]
    )
    indexes = []
    request_pb = request_pb2.RpcRequest(
        get_reports_info=reports_pb2.GetReportsInfo(
            agency_id=fixture_reports[0].agency_id,
            **rpc_request_kwargs
        )
    )

    data = await handler(request_pb.SerializeToString())
    message = reports_pb2.GetReportsInfoOutput.FromString(data)
    res = structs.GetReportsInfoResponse.from_proto(message.result)

    for idx, report in zip(indexes, expected.reports):
        report.report_id = fixture_reports[idx].id
    assert res == expected


@pytest.mark.skip(reason="Real models shouldn't used here")
async def test_ord_get_detailed_report_info(
    handler,
    fixture_reports: typing.List[models.Report],
):
    request_pb = request_pb2.RpcRequest(
        get_detailed_report_info=reports_pb2.GetDetailedReportInfo(
            agency_id=fixture_reports[0].agency_id,
            report_id=fixture_reports[0].id
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = reports_pb2.GetDetailedReportInfoOutput.FromString(data)

    res = structs.ReportInfo.from_proto(message.result)
    expected = structs.ReportInfo(
        report_id=fixture_reports[0].id,
        status=structs.ReportStatuses.sent,
        ad_distributor_id=fixture_reports[0].ad_distributor_id,
        ad_distributor_inn='yandex inn',
        ad_distributor_name='yandex',
        ad_distributor_display_name='Яндекс',
        ad_distributor_settings={},
        contract_eid='12345',
        currency=Currencies.rub,
        vat=20,
        period_from=datetime.datetime(2021, 1, 1, 0, 0, tzinfo=datetime.timezone.utc),
        sending_date=datetime.datetime(2021, 2, 1, 0, 0, tzinfo=datetime.timezone.utc),
        amount=Decimal('20'),
        suggested_amount=Decimal('20'),
        clients_count=2,
        agency_inn=fixture_reports[0].agency_inn,
        ad_distributors_acts_count=0,
        ad_distributors_acts_sum_amount=Decimal(0)
    )
    assert res == expected


@pytest.mark.skip(reason="Real models shouldn't used here")
async def test_ord_get_detailed_report_info_no_such_report(
    handler,
    fixture_reports: typing.List[models.Report],
):
    request_pb = request_pb2.RpcRequest(
        get_detailed_report_info=reports_pb2.GetDetailedReportInfo(
            agency_id=1,
            report_id=9999
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = reports_pb2.GetDetailedReportInfoOutput.FromString(data)
    expected = structs.ErrorMessageResponse(
        message='Can\'t find report with id 9999'
    )
    assert message.no_such_report is not None
    assert structs.ErrorMessageResponse.from_proto(message.no_such_report) == expected


@pytest.mark.skip(reason="Real models shouldn't used here")
async def test_ord_get_detailed_report_info_unsuitable_agency(
    handler,
    fixture_reports: typing.List[models.Report],
):
    request_pb = request_pb2.RpcRequest(
        get_detailed_report_info=reports_pb2.GetDetailedReportInfo(
            agency_id=9999,
            report_id=fixture_reports[0].id
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = reports_pb2.GetDetailedReportInfoOutput.FromString(data)
    expected = structs.ErrorMessageResponse(
        message=f'Unsuitable agency for report with id {fixture_reports[0].id}'
    )
    assert message.unsuitable_agency is not None
    assert structs.ErrorMessageResponse.from_proto(message.unsuitable_agency) == expected
