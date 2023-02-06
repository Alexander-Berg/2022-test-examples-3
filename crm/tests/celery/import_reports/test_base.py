import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.ord.server.src.celery.tasks.transfer.import_reports.base import BaseReportImporter
from crm.agency_cabinet.ord.server.src.config import MDS_SETTINGS, REPORTS_MDS_SETTINGS
from crm.agency_cabinet.ord.server.src.db import models


@pytest.fixture
def base_report_importer(mds_cfg, fixture_celery_report_import):
    return BaseReportImporter(
        MDS_SETTINGS,
        REPORTS_MDS_SETTINGS,
        fixture_celery_report_import[0].id,
        fixture_celery_report_import[0].report_id,
        1,
        'mds_filename'
    )


@pytest.fixture(autouse=True)
def load_mds_file(mocker):
    return mocker.patch(
        'crm.agency_cabinet.ord.server.src.celery.tasks.transfer.import_reports.base.BaseReportImporter._load_mds_file',
        mock=AsyncMock,
        side_effect=Exception()
    )


async def test_import_report_error_status(
    base_report_importer,
    fixture_celery_report_import,
    load_mds_file,
):
    try:
        await base_report_importer.import_report()
    except:
        pass

    import_task = await models.ReportImportInfo.query.where(
        models.ReportImportInfo.id == fixture_celery_report_import[0].id
    ).gino.first()
    assert import_task.status == TaskStatuses.error.value
