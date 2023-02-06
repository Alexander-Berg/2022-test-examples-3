import pytest
import typing
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common.exceptions import NoSuchReportException, UnsuitableAgencyException, ImportDataAlreadyRequested


@pytest.fixture
def procedure():
    return procedures.ImportData()


async def test_import_data(procedure, fixture_reports2: typing.List[RowProxy]):
    # with patch('crm.agency_cabinet.ord.server.src.procedures.import_data.import_report_task.delay'):
    await procedure(structs.ImportDataInput(
        agency_id=1,
        report_id=fixture_reports2[1].id,
        filename='filename',
        bucket='bucket'
    ))

    task = await models.ReportImportInfo.query.where(models.ReportImportInfo.report_id == fixture_reports2[1].id).gino.first()
    assert task is not None
    assert task.status == TaskStatuses.requested.value

    with pytest.raises(ImportDataAlreadyRequested):
        await procedure(structs.ImportDataInput(
            agency_id=1,
            report_id=fixture_reports2[1].id,
            filename='filename',
            bucket='bucket'
        ))

    await task.delete()


async def test_import_data_unsuitable_agency(procedure, fixture_reports2: typing.List[RowProxy]):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(structs.ImportDataInput(
            agency_id=404,
            report_id=fixture_reports2[1].id,
            filename='filename',
            bucket='bucket'
        ))


async def test_import_data_no_such_report(procedure):
    with pytest.raises(NoSuchReportException):
        await procedure(structs.ImportDataInput(
            agency_id=1,
            report_id=404,
            filename='filename',
            bucket='bucket'
        ))
