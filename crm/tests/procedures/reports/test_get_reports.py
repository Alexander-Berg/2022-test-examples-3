import pytest
import typing
from sqlalchemy.engine.result import RowProxy

from smb.common.testing_utils import dt

from crm.agency_cabinet.ord.common import consts, structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models


@pytest.fixture
def procedure():
    return procedures.GetReportsInfo()


async def test_select_reports(procedure, fixture_reports2: typing.List[RowProxy]):
    got = await procedure(structs.GetReportsInfoRequest(
        agency_id=1,
        sort=[],
        limit=2
    ))

    assert got == structs.GetReportsInfoResponse(
        reports=[
            structs.ReportInfo(
                report_id=fixture_reports2[0].id,
                period_from=dt('2021-3-1 00:00:00'),
                status=consts.ReportStatuses.draft,
                reporter_type=consts.ReporterType.partner,
                clients_count=0,
                campaigns_count=0,
                sending_date=None,
                settings=structs.ReportSettings(
                    name='direct',
                    display_name='Директ',
                    allow_create_ad_distributor_acts=False,
                    allow_create_clients=False,
                    allow_create_campaigns=False,
                    allow_edit_report=False,
                )
            ),
            structs.ReportInfo(
                report_id=fixture_reports2[1].id,
                period_from=dt('2021-4-1 00:00:00'),
                status=consts.ReportStatuses.draft,
                reporter_type=consts.ReporterType.partner,
                clients_count=0,
                campaigns_count=0,
                sending_date=None,
                settings=structs.ReportSettings(
                    name='direct',
                    display_name='Директ',
                    allow_create_ad_distributor_acts=False,
                    allow_create_clients=False,
                    allow_create_campaigns=False,
                    allow_edit_report=False,
                )
            ),
        ]
    )


@pytest.fixture()
async def fixture_reports3(fixture_report_settings):
    rows = [
        {
            'agency_id': 1,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': dt('2021-3-1 00:00:00'),
            'status': 'draft',
            'is_deleted': False,
        },
        {
            'agency_id': 1,
            'settings_id': fixture_report_settings[1].id,
            'reporter_type': 'partner',
            'period_from': dt('2021-3-1 00:00:00'),
            'status': 'draft',
            'is_deleted': True,
        },
    ]
    yield await models.Report.bulk_insert(rows)

    await models.Report.delete.gino.status()


async def test_select_reports_exclude_deleted(procedure, fixture_reports3: typing.List[RowProxy]):
    got = await procedure(structs.GetReportsInfoRequest(
        agency_id=1,
        sort=[],
        limit=2
    ))

    assert got == structs.GetReportsInfoResponse(
        reports=[
            structs.ReportInfo(
                report_id=fixture_reports3[0].id,
                period_from=dt('2021-3-1 00:00:00'),
                status=structs.ReportStatuses.draft,
                reporter_type=consts.ReporterType.partner,
                clients_count=0,
                campaigns_count=0,
                sending_date=None,
                settings=structs.ReportSettings(
                    name='direct',
                    display_name='Директ',
                    allow_create_ad_distributor_acts=False,
                    allow_create_clients=False,
                    allow_create_campaigns=False,
                    allow_edit_report=False,
                )
            ),
        ]
    )
