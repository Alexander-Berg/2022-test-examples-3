import pytest
import typing
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.common.server.common.structs import TaskStatuses


@pytest.fixture
def procedure():
    return procedures.GetReportExportInfo()


async def test_get_report_export(
    procedure,
    fixture_reports2: typing.List[RowProxy],
    fixture_report_export: typing.List[RowProxy]
):
    got = await procedure(structs.ReportExportInfoRequest(
        agency_id=1,
        report_id=fixture_reports2[1].id,
        report_export_id=fixture_report_export[1].id,
    ))

    assert got == structs.ReportExportResponse(
        report_export_id=fixture_report_export[1].id,
        status=TaskStatuses(fixture_report_export[1].status)
    )
