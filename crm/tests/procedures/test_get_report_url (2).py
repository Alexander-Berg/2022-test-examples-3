import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, FileNotFoundException, \
    NoSuchReportException, ReportNotReadyException

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure():
    return procedures.GetReportUrl()


@pytest.fixture
def s3_client(mocker):
    return mocker.patch(
        'crm.agency_cabinet.ord.server.src.procedures.reports.create_presigned_url_async',
        mock=AsyncMock,
        return_value='test.scv'
    )


async def test_returns_report_url(procedure, s3_client, fixture_reports2, fixture_s3_mds_file,
                                  fixture_report_export):

    got = await procedure(structs.GetReportUrlRequest(
        agency_id=1,
        report_id=fixture_reports2[1].id,
        report_export_id=fixture_report_export[1].id
    ),
        s3_client
    )

    expected = structs.GetReportUrlResponse(report_url='test.scv')

    assert got == expected


async def test_unsuitable_agency(procedure, s3_client, fixture_reports2, fixture_s3_mds_file, fixture_report_export):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(structs.GetReportUrlRequest(
            agency_id=2,
            report_id=fixture_reports2[1].id,
            report_export_id=fixture_report_export[1].id
        ),
            s3_client
        )


async def test_report_not_ready(procedure, s3_client, fixture_reports2, fixture_s3_mds_file, fixture_report_export):
    with pytest.raises(ReportNotReadyException):
        await procedure(structs.GetReportUrlRequest(
            agency_id=1,
            report_id=fixture_reports2[1].id,
            report_export_id=fixture_report_export[2].id
        ),
            s3_client
        )


async def test_file_not_found(procedure, fixture_reports2, fixture_s3_mds_file, fixture_report_export):
    with pytest.raises(FileNotFoundException):
        await procedure(structs.GetReportUrlRequest(
            agency_id=1,
            report_id=fixture_reports2[1].id,
            report_export_id=fixture_report_export[3].id
        ),
            s3_client)


async def test_no_such_report(procedure, fixture_reports2, fixture_s3_mds_file, fixture_report_export):
    with pytest.raises(NoSuchReportException):
        await procedure(structs.GetReportUrlRequest(
            agency_id=1,
            report_id=0,
            report_export_id=0
        ),
            s3_client)
