import pytest

from mail.payments.payments.tests.base import BaseTestDocumentByPathDownload

from .base import BaseTestNotAuthorized


class TestDocumentDownloadAdmin(BaseTestNotAuthorized, BaseTestDocumentByPathDownload):
    @pytest.fixture
    def tvm_uid(self, manager_admin):
        return manager_admin.uid

    @pytest.fixture
    async def download_response(self, admin_client, merchant, path, tvm):
        return await admin_client.get(f'/admin/api/v1/document/{merchant.uid}/{path}/download')

    @pytest.fixture
    def response(self, download_response):  # для BaseTestNotAuthorized
        return download_response

    @pytest.mark.asyncio
    async def test_returns_joined_chunks(self, chunks, response_data):
        assert response_data == b''.join(chunks)

    class TestNotFound:
        @pytest.fixture
        def path(self):
            return 'test-document-download-not-found-path'

        @pytest.mark.asyncio
        async def test_returns_not_found_error(self, not_found_error):
            assert not_found_error == 'DOCUMENT_NOT_FOUND'

    class TestNotAllowedManager:
        @pytest.fixture
        def tvm_uid(self, manager_assessor):
            return manager_assessor.uid

        def test_not_allowed(self, download_response):
            assert download_response.status == 403
