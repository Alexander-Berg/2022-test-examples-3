import pytest

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestDocumentByPathDownload, BaseTestMerchantRoles


class TestDocumentByPathDownload(BaseTestMerchantRoles, BaseTestDocumentByPathDownload):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    async def download_response(self, client, merchant, path, tvm):
        return await client.get(f'/v1/document/{merchant.uid}/{path}/download')

    @pytest.mark.asyncio
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
