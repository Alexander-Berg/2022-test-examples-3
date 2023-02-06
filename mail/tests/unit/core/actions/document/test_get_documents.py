import pytest

from mail.payments.payments.core.actions.document import GetDocumentsAction
from mail.payments.payments.core.entities.document import Document, DocumentType


class TestGetDocuments:
    @pytest.fixture
    def merchant_documents(self):
        return [
            Document(
                document_type=DocumentType.PASSPORT,
                path='test-get-documents-path',
                size=1234,
                name='test-get-documents',
            )
        ]

    @pytest.fixture(params=('uid', 'merchant'))
    def params(self, request, merchant):
        data = {'uid': merchant.uid, 'merchant': merchant}
        return {request.param: data[request.param]}

    @pytest.fixture
    async def returned(self, params):
        return await GetDocumentsAction(**params).run()

    def test_returns_document(self, merchant, returned):
        assert returned == merchant.documents
