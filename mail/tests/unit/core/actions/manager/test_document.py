import pytest

from mail.payments.payments.core.actions.document import DownloadDocumentAction
from mail.payments.payments.core.actions.manager.document import DownloadDocumentManagerAction
from mail.payments.payments.core.exceptions import ManagerActionNotAllowed


@pytest.fixture
def manager_uid(manager_admin):
    return manager_admin.uid


@pytest.fixture
def params(unique_rand, randn, manager_uid):
    return {
        'path': 'some_path',
        'uid': unique_rand(randn, basket='uid'),
        'manager_uid': manager_uid,
    }


class TestDownloadDocumentManagerAction:
    @pytest.fixture(autouse=True)
    def download_document(self, mock_action, content_type, data):
        return mock_action(DownloadDocumentAction, (content_type, data))

    @pytest.fixture
    async def returned(self, params):
        return await DownloadDocumentManagerAction(**params).run()

    def test_returned(self, returned, content_type, data):
        assert returned == (content_type, data)

    def test_download_document_calls(self, download_document, returned, params):
        params.pop('manager_uid')
        download_document.assert_called_once_with(**params)


class TestDownloadDocumentActionDeniedForAssessor:
    @pytest.fixture
    def manager_uid(self, manager_assessor):
        return manager_assessor.uid

    @pytest.mark.asyncio
    async def test_deny_exception(self, params):
        with pytest.raises(ManagerActionNotAllowed):
            await DownloadDocumentManagerAction(**params).run()
