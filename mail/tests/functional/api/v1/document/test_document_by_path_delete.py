import pytest

from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


class TestDocumentByPathDelete(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture(autouse=True)
    def mds_remove_mock(self, mds_client_mocker):
        with mds_client_mocker('remove', None) as mock:
            yield mock

    @pytest.fixture
    def merchant_documents(self, randn):
        return [
            Document(
                document_type=DocumentType.PROXY,
                path='test-document-by-path-delete-path',
                size=randn(),
                name='test-document-by-path-delete-name',
            )
        ]

    @pytest.fixture
    def path(self, merchant_documents):
        return merchant_documents[0].path

    @pytest.fixture
    async def delete_response(self, client, merchant, path, tvm):
        response = await client.delete(f'/v1/document/{merchant.uid}/{path}')
        return (await response.json())['data']

    @pytest.fixture
    async def updated_merchant(self, storage, merchant, delete_response):
        return await storage.merchant.get(merchant.uid)

    def test_deletes(self, path, updated_merchant):
        assert path not in [d.path for d in updated_merchant.documents]

    class TestNotFound:
        @pytest.fixture
        def path(self):
            return 'test-document-delete-not-found-path'

        def test_returns_not_found_error(self, delete_response):
            assert delete_response['message'] == 'DOCUMENT_NOT_FOUND'
