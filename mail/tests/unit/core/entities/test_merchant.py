import pytest

from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.entities.enums import AcquirerType, RegistrationRoute
from mail.payments.payments.core.exceptions import TinkoffInvalidSubmerchantIdError
from mail.payments.payments.tests.base import BaseAcquirerTest


class TestMerchant:
    class TestGetDocumentsByType:
        @pytest.fixture
        def merchant_documents(self, randn):
            return [
                Document(
                    document_type=DocumentType.PASSPORT,
                    path='test-merchant-get-documents-by-type',
                    size=randn(),
                    name='test-merchant-get-documents-by-type',
                ),
                Document(
                    document_type=DocumentType.PASSPORT,
                    path='test-merchant-get-documents-by-type',
                    size=randn(),
                    name='test-merchant-get-documents-by-type',
                ),
                Document(
                    document_type=DocumentType.OFFER,
                    path='test-merchant-get-documents-by-type',
                    size=randn(),
                    name='test-merchant-get-documents-by-type',
                ),
                Document(
                    document_type=DocumentType.PROXY,
                    path='test-merchant-get-documents-by-type',
                    size=randn(),
                    name='test-merchant-get-documents-by-type',
                ),
                Document(
                    document_type=DocumentType.OFFER,
                    path='test-merchant-get-documents-by-type',
                    size=randn(),
                    name='test-merchant-get-documents-by-type',
                ),
            ]

        def test_get_documents_by_type__result(self, merchant):
            assert merchant.get_documents_by_type() == {
                DocumentType.PASSPORT: [merchant.documents[0], merchant.documents[1]],
                DocumentType.OFFER: [merchant.documents[2], merchant.documents[4]],
                DocumentType.PROXY: [merchant.documents[3]],
            }


class TestAcquireTypeToRegistrationRouteProperty:
    def test_tinkoff(self, merchant):
        merchant.acquirer = AcquirerType.TINKOFF
        assert merchant.registration_route == RegistrationRoute.TINKOFF

    def test_kassa(self, merchant):
        merchant.acquirer = AcquirerType.KASSA
        assert merchant.registration_route == RegistrationRoute.KASSA

    def test_none(self, merchant):
        merchant.acquirer = None
        assert merchant.registration_route == RegistrationRoute.OFFLINE


class TestGetSubmerchantId(BaseAcquirerTest):
    def test_returned(self, merchant, rands):
        merchant.submerchant_id = rands()
        assert merchant.get_submerchant_id() == merchant.submerchant_id

    def test_exception(self, merchant, acquirer, noop_manager):
        merchant.submerchant_id = None
        manager = pytest.raises if acquirer == AcquirerType.TINKOFF else noop_manager

        with manager(TinkoffInvalidSubmerchantIdError):
            assert merchant.get_submerchant_id() is None


def test_get_submerchant_id(merchant, rands):
    merchant.submerchant_id = rands()
    assert merchant.get_submerchant_id() == merchant.submerchant_id
