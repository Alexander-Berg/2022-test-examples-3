import pytest

from mail.payments.payments.core.actions.document import DeleteDocumentAction
from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.exceptions import CoreFailError, DocumentNotFoundError
from mail.payments.payments.tests.base import BaseTestRequiresNoModeration


class TestDeleteDocument(BaseTestRequiresNoModeration):
    @pytest.fixture(autouse=True)
    def mds_remove_mock(self, mds_client_mocker):
        with mds_client_mocker('remove', None) as mock:
            yield mock

    @pytest.fixture
    def path(self):
        return 'test-delete-document-path'

    @pytest.fixture
    def merchant_documents(self, path):
        return [
            Document(
                document_type=DocumentType.PASSPORT,
                path=path,
                size=1234,
                name='test-delete-document',
            ),
            Document(
                document_type=DocumentType.PASSPORT,
                path='test-delete-document-other-path',
                size=5678,
                name='test-delete-document-other',
            ),
        ]

    @pytest.fixture(params=('uid', 'merchant'))
    def params(self, request, merchant, path):
        data = {'uid': merchant.uid, 'merchant': merchant}
        return {
            'path': path,
            request.param: data[request.param],
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await DeleteDocumentAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def updated_merchant(self, storage, merchant, returned):
        return await storage.merchant.get(merchant.uid)

    @pytest.mark.asyncio
    async def test_raises_not_found(self, params):
        params['path'] = 'test-delete-document-not-found-path'
        with pytest.raises(DocumentNotFoundError):
            await DeleteDocumentAction(**params).run()

    def test_deletes_document(self, path, merchant_documents, updated_merchant):
        assert updated_merchant.documents == [d for d in merchant_documents if d.path != path]

    def test_mds_remove_call(self, mds_remove_mock, path, returned):
        mds_remove_mock.assert_called_once_with(path)

    class TestDuplicatePaths:
        @pytest.fixture
        def merchant_documents(self, path):
            return [Document(
                document_type=DocumentType.PASSPORT,
                path=path,
                size=1234,
                name='test_duplicate_paths_documents',
            )] * 2

        @pytest.mark.asyncio
        async def test_raises_fail_error(self, params):
            with pytest.raises(CoreFailError):
                await DeleteDocumentAction(**params).run()
