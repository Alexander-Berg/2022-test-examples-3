import pytest

from mail.payments.payments.core.actions.report import DownloadReportAction
from mail.payments.payments.core.entities.report import Report
from mail.payments.payments.core.exceptions import ReportNotFoundError, ReportNotUploadedError
from mail.payments.payments.utils.datetime import utcnow


class TestDownloadReport:
    @pytest.fixture
    def mds_path(self):
        return 'test-download-report-path'

    @pytest.fixture
    def content_type(self):
        return 'test-download-report-content-type'

    @pytest.fixture
    def data(self):
        return 'test-download-report-data'

    @pytest.fixture(autouse=True)
    def download_mock(self, mds_client_mocker, content_type, data):
        with mds_client_mocker('download', (content_type, data)) as mock:
            yield mock

    @pytest.fixture
    def created(self):
        return utcnow()

    @pytest.fixture
    async def report_entity(self, storage, mds_path, merchant, created):
        return Report(mds_path=mds_path, uid=merchant.uid, created=created)

    @pytest.fixture
    async def report(self, storage, report_entity):
        return await storage.report.create(report_entity)

    @pytest.fixture
    def report_id(self, report):
        return report.report_id

    @pytest.fixture
    def params(self, merchant, report_id):
        return {
            'uid': merchant.uid,
            'report_id': report_id,
        }

    @pytest.fixture
    async def returned(self, report, params):
        return await DownloadReportAction(**params).run()

    @pytest.mark.asyncio
    async def test_raises_not_found(self, merchant, params):
        params['report_id'] = 'test-download-report-not-found-report-id'
        params['uid'] = merchant.uid
        with pytest.raises(ReportNotFoundError):
            await DownloadReportAction(**params).run()

    @pytest.mark.asyncio
    async def test_download_call(self, storage, mds_path, merchant, download_mock, returned):
        download_mock.assert_called_once_with(mds_path, close=True)

    @pytest.fixture
    async def not_loaded_report_entity(self, storage, mds_path, merchant, created):
        return Report(mds_path='', uid=merchant.uid, created=created)

    @pytest.fixture
    async def not_loaded_report(self, storage, not_loaded_report_entity):
        return await storage.report.create(not_loaded_report_entity)

    @pytest.mark.asyncio
    async def test_not_loaded(self, not_loaded_report, merchant, params):
        params['report_id'] = not_loaded_report.report_id
        with pytest.raises(ReportNotUploadedError):
            await DownloadReportAction(**params).run()
