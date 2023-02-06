import pytest
import typing
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.common.exceptions import NoSuchReportException, UnsuitableAgencyException


@pytest.fixture
def procedure():
    return procedures.GetLockStatus()


async def test_get_lock_status(procedure, fixture_reports2: typing.List[RowProxy]):
    res = await procedure(structs.GetLockStatusInput(
        agency_id=1,
        report_id=fixture_reports2[1].id,
    ))

    assert res.lock is False


async def test_get_lock_status_unsuitable_agency(procedure, fixture_reports2: typing.List[RowProxy]):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(structs.GetLockStatusInput(
            agency_id=404,
            report_id=fixture_reports2[1].id,
        ))


async def test_get_lock_status_no_such_report(procedure):
    with pytest.raises(NoSuchReportException):
        await procedure(structs.GetLockStatusInput(
            agency_id=1,
            report_id=404,
        ))
