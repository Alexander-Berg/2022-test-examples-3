import random
from collections import defaultdict

import pytest

from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


class TestDocumentsGet(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture(params=[
        [],
        [
            Document(
                document_type=DocumentType.PASSPORT,
                path='test-document-get-passport-path',
                size=random.randint(1, 10**6),
                name='test-document-get-passport-name',
            ),
            Document(
                document_type=DocumentType.PASSPORT,
                path='test-document-get-passport-other-path',
                size=random.randint(1, 10**6),
                name='test-document-get-passport-other-name',
            ),
            Document(
                document_type=DocumentType.OFFER,
                path='test-document-get-offer-path',
                size=random.randint(1, 10**6),
                name='test-document-get-offer-name',
            ),
        ],
    ])
    def merchant_documents(self, request):
        return request.param

    @pytest.fixture
    async def documents_response(self, client, merchant, tvm):
        response = await client.get(f'/v1/document/{merchant.uid}')
        assert response.status == 200
        return (await response.json())['data']

    def test_data(self, merchant_documents, documents_response):
        data = defaultdict(list)
        for d in merchant_documents:
            data[d.document_type.value].append({
                'path': d.path,
                'size': d.size,
                'created': d.created.isoformat(),
                'name': d.name,
            })
        assert documents_response == data
