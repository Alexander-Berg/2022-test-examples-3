import pytest
from unittest.mock import AsyncMock

from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import GetReportUrlResponse, ClientType
from crm.agency_cabinet.client_bonuses.server.lib.procedures import GetReportUrl
from crm.agency_cabinet.client_bonuses.server.lib.exceptions import UnsuitableAgencyException, FileNotFoundException, \
    NoSuchReportException, ReportNotReadyException
from crm.agency_cabinet.common.consts.report import ReportsStatuses

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure(db):
    return GetReportUrl(db)


@pytest.fixture
async def default_client(factory):
    return await factory.create_client(id_=42)


@pytest.fixture
async def s3_mds_file(default_client, factory):
    return await factory.create_s3_mds_file(
        name='test',
        bucket='test_bucket',
        display_name='test_display_name'
    )


@pytest.fixture
async def report(default_client, s3_mds_file, factory):
    return await factory.create_report_meta_info(
        name='test',
        agency_id=default_client['agency_id'],
        period_from=dt("2020-03-02 18:00:00"),
        period_to=dt("2020-08-02 18:00:00"),
        client_type=ClientType.ALL,
        status=ReportsStatuses.ready.value,
        file_id=s3_mds_file['id']
    )


@pytest.fixture
async def report_not_ready(default_client, factory):
    return await factory.create_report_meta_info(
        name='test not ready',
        agency_id=default_client['agency_id'],
        period_from=dt("2020-03-02 18:00:00"),
        period_to=dt("2020-08-02 18:00:00"),
        client_type=ClientType.ALL,
        status=ReportsStatuses.requested.value,
        file_id=None
    )


@pytest.fixture
async def report_without_file(default_client, factory):
    return await factory.create_report_meta_info(
        name='test not ready',
        agency_id=default_client['agency_id'],
        period_from=dt("2020-03-02 18:00:00"),
        period_to=dt("2020-08-02 18:00:00"),
        client_type=ClientType.ALL,
        status=ReportsStatuses.ready.value,
        file_id=None
    )


async def test_returns_report_url(procedure, report, mocker):
    url = 'http://example.url'
    mocker.patch(
        'crm.agency_cabinet.client_bonuses.server.lib.procedures.create_presigned_url_async',
        mock=AsyncMock,
        return_value=url)

    res = await procedure(report['agency_id'], report['id'], None)

    assert res == GetReportUrlResponse(report_url=url)


async def test_unsuitable_agency(procedure, report):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(404, report['id'], None)


async def test_report_not_ready(procedure, report_not_ready):
    with pytest.raises(ReportNotReadyException):
        await procedure(report_not_ready['agency_id'], report_not_ready['id'], None)


async def test_file_not_found(procedure, report_without_file):
    with pytest.raises(FileNotFoundException):
        await procedure(report_without_file['agency_id'], report_without_file['id'], None)


async def test_no_such_report(procedure):
    with pytest.raises(NoSuchReportException):
        await procedure(404, 404, None)
