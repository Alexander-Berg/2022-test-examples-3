import pytest

from mail.payments.payments.core.entities.report import Report
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def merchant_uid(unique_rand, randn):
    return unique_rand(randn, basket='uid')


@pytest.fixture
def mds_path():
    return 'asd'


@pytest.fixture
def report_id():
    return 'test-report-id'


@pytest.fixture
def created():
    return utcnow()


@pytest.fixture
async def report_entity(storage, mds_path, merchant, report_id, created):
    return Report(report_id=report_id, mds_path=mds_path, uid=merchant.uid, created=created)


@pytest.fixture
async def report(storage, merchant, report_entity):
    return await storage.report.create(report_entity)


@pytest.mark.asyncio
async def test_report_created(report_entity, report):
    assert report_entity == report


@pytest.mark.asyncio
async def test_get_report(report, storage):
    assert report == await storage.report.get(report_id=report.report_id)


@pytest.mark.asyncio
async def test_find_report_basic(report, storage):
    assert [r async for r in storage.report.find()] == [report]


@pytest.mark.asyncio
async def test_save_report(report, storage):
    path = 'new-report-path'
    report.mds_path = path
    await storage.report.save(report)
    assert path == (await storage.report.get(report_id=report.report_id)).mds_path
