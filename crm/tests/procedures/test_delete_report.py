import pytest
import typing
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common.exceptions import NoSuchReportException, UnsuitableAgencyException


@pytest.fixture
def procedure():
    return procedures.DeleteReport()


async def test_delete_report(procedure, fixture_reports2: typing.List[RowProxy], fixture_clients: typing.List[RowProxy]):
    await procedure(structs.DeleteReportRequest(
        agency_id=1,
        report_id=fixture_reports2[1].id,
    ))

    report = await models.Report.query.where(models.Report.id == fixture_reports2[1].id).gino.first()
    assert report.is_deleted is True

    await report.delete()


async def test_delete_report_unsuitable_agency(procedure, fixture_reports2: typing.List[RowProxy], fixture_clients: typing.List[RowProxy]):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(structs.DeleteReportRequest(
            agency_id=404,
            report_id=fixture_reports2[1].id,
        ))


async def test_delete_report_no_such_report(procedure, fixture_clients: typing.List[RowProxy]):
    with pytest.raises(NoSuchReportException):
        await procedure(structs.DeleteReportRequest(
            agency_id=1,
            report_id=404,
        ))
