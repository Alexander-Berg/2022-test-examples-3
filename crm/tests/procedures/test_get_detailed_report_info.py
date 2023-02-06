import pytest

from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import GetDetailedReportInfoResponse, ReportInfo, ClientType
from crm.agency_cabinet.client_bonuses.server.lib.procedures import GetDetailedReportInfo
from crm.agency_cabinet.client_bonuses.server.lib.exceptions import UnsuitableAgencyException, NoSuchReportException
from crm.agency_cabinet.common.consts.report import ReportsStatuses

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure(db):
    return GetDetailedReportInfo(db)


@pytest.fixture
async def default_client(factory):
    return await factory.create_client(id_=42)


@pytest.fixture
async def report(default_client, factory):
    return await factory.create_report_meta_info(
        name='test',
        agency_id=default_client['agency_id'],
        period_from=dt("2020-03-02 18:00:00"),
        period_to=dt("2020-08-02 18:00:00"),
        client_type=ClientType.ALL,
        status=ReportsStatuses.ready.value,
        file_id=None
    )


async def test_get_detailed_report_info(procedure, report):
    res = await procedure(report['agency_id'], report['id'])
    assert res == GetDetailedReportInfoResponse(report=ReportInfo(
        id=report['id'],
        name=report['name'],
        period_from=report['period_from'],
        period_to=report['period_to'],
        created_at=report['created_at'],
        status=report['status'],
        client_type=ClientType(report['client_type'])
    ))


async def test_unsuitable_agency(procedure, report):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(404, report['id'])


async def test_no_such_report(procedure):
    with pytest.raises(NoSuchReportException):
        await procedure(404, 404)
