import pytest
import typing

from smb.common.testing_utils import dt

from crm.agency_cabinet.ord.common import structs, consts
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models


@pytest.fixture
def procedure():
    return procedures.CreateReport()


async def test_create_report(procedure, fixture_report_settings: typing.List[models.ReportSettings]):
    created = await procedure(structs.CreateReportRequest(
        agency_id=1,
        period_from=dt('2222-3-1 00:00:00'),
        reporter_type=consts.ReporterType.partner,
    ))

    report = await models.Report.query.where(models.Report.period_from == dt('2222-3-1 00:00:00')).gino.first()
    assert created == structs.ReportInfo(
        report_id=report.id,
        period_from=report.period_from,
        status=consts.ReportStatuses.draft,
        reporter_type=consts.ReporterType.partner,
        clients_count=0,
        campaigns_count=0,
        sending_date=None,
        settings=structs.ReportSettings(
            name='other',
            display_name='Другое',
            allow_create_ad_distributor_acts=True,
            allow_create_clients=True,
            allow_create_campaigns=True,
            allow_edit_report=True,
        )
    )

    await report.delete()
