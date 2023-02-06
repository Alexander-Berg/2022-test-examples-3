import pytest
import crm.agency_cabinet.ord.common.structs as ord_structs
from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.gateway.server.src.procedures.ord.reports import ReportExport, ReportExportInfo
from crm.agency_cabinet.grants.common.structs import AccessLevel

REPORT = [
    ord_structs.ReportExportResponse(
        report_export_id=1,
        status=TaskStatuses.requested.value,
    )
]


@pytest.fixture
def procedure_report_export(service_discovery):
    return ReportExport(service_discovery)


@pytest.fixture
def procedure_get_report_export_info(service_discovery):
    return ReportExportInfo(service_discovery)


async def test_returns_export_report(procedure_report_export, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.ord.report_export.return_value = REPORT

    got = await procedure_report_export(yandex_uid=123, agency_id=22, report_id=1)

    assert got == REPORT


async def test_returns_export_report_info(procedure_get_report_export_info, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.ord.get_report_export_info.return_value = REPORT

    got = await procedure_get_report_export_info(yandex_uid=123, agency_id=22, report_id=1, report_export_id=1)

    assert got == REPORT
