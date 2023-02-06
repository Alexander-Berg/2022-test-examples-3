import pytest

from mail.payments.payments.core.actions.report import GetReportListAction
from mail.payments.payments.core.entities.report import Report
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def mds_path():
    return 'test-reports-list-path'


@pytest.fixture
def created():
    return utcnow()


@pytest.fixture
async def reports(storage, merchant, mds_path, created):
    return [
        await storage.report.create(Report(
            report_id=str(i),
            mds_path=mds_path,
            uid=merchant.uid,
            created=created,
        ))
        for i in range(3)
    ]


@pytest.fixture
def params(merchant):
    return {'uid': merchant.uid}


@pytest.fixture
async def returned(reports, params):
    return await GetReportListAction(**params).run()


def test_reports_list_returned(returned, reports):
    assert reports == returned
