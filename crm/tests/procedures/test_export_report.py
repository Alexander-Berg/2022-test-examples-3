import pytest
import typing
from sqlalchemy.engine.result import RowProxy

from smb.common.testing_utils import Any
from smb.common.testing_utils import dt
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.common.server.common.structs import TaskStatuses


@pytest.fixture
def procedure():
    return procedures.ReportExport()


@pytest.fixture
async def fixture_report_export2(fixture_reports2, fixture_s3_mds_file):
    rows = [
        {
            'report_id': fixture_reports2[1].id,
            'file_id': fixture_s3_mds_file[1].id,
            'status': 'ready',
            'created_at': dt('3333-3-3 00:00:00')
        },
    ]

    yield await models.ReportExportInfo.bulk_insert(rows)
    await models.ReportExportInfo.delete.gino.status()


async def test_report_export(
    procedure,
    fixture_reports2: typing.List[RowProxy],
):
    await procedure(structs.ReportExportRequest(
        agency_id=1,
        report_id=fixture_reports2[1].id,
    ))

    row = await models.ReportExportInfo.query.gino.first()

    assert row.id == Any(int) and row.status == 'requested'


async def test_report_export_already_exported(
    procedure,
    fixture_reports2: typing.List[RowProxy],
    fixture_report_export2: typing.List[RowProxy],
):
    exported_report = await procedure(structs.ReportExportRequest(
        agency_id=1,
        report_id=fixture_reports2[1].id,
    ))

    assert exported_report == structs.ReportExportResponse(
        report_export_id=fixture_report_export2[0].id,
        status=TaskStatuses(fixture_report_export2[0].status)
    )

    exports = await models.ReportExportInfo.query.gino.all()
    assert len(exports) == 1


async def test_report_export_empty(
    procedure,
    fixture_reports2: typing.List[RowProxy]
):
    await procedure(structs.ReportExportRequest(
        agency_id=1,
        report_id=fixture_reports2[1].id,
    ))

    row = await models.ReportExportInfo.query.gino.first()

    assert row.id == Any(int) and row.status == 'requested'
