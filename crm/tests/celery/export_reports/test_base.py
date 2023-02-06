import pytest
import tempfile
from unittest.mock import AsyncMock

from crm.agency_cabinet.common.server.common.structs import TaskStatuses
from crm.agency_cabinet.ord.server.src.celery.tasks.transfer.export_reports.base import BaseReportExporter
from crm.agency_cabinet.ord.server.src.config import MDS_SETTINGS, REPORTS_MDS_SETTINGS
from crm.agency_cabinet.ord.server.src.db import models


@pytest.fixture
def base_report_exporter(mds_cfg, fixture_celery_report_export):
    return BaseReportExporter(
        MDS_SETTINGS,
        REPORTS_MDS_SETTINGS,
        fixture_celery_report_export[0].id,
        fixture_celery_report_export[0].report_id
    )


@pytest.fixture(autouse=False)
def _make_report(mocker):
    return mocker.patch(
        'crm.agency_cabinet.ord.server.src.celery.tasks.transfer.export_reports.base.BaseReportExporter._make_report',
        mock=AsyncMock,
        return_value=tempfile.NamedTemporaryFile(suffix=".xlsx")
    )


@pytest.fixture(autouse=False)
def _upload_to_mds_exception(mocker):
    return mocker.patch(
        'crm.agency_cabinet.ord.server.src.celery.tasks.transfer.export_reports.base.BaseReportExporter._upload_to_mds',
        mock=AsyncMock,
        side_effect=Exception()
    )


async def test_export_report_error_status(
    base_report_exporter,
    fixture_celery_report_export,
    _upload_to_mds_exception,
    _make_report
):
    try:
        await base_report_exporter.generate()
    except:
        pass

    export_task = await models.ReportExportInfo.query.where(
        models.ReportExportInfo.id == fixture_celery_report_export[0].id
    ).gino.first()
    assert export_task.status == TaskStatuses.error.value
