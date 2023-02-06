import pytest

from mail.payments.payments.core.entities.document import Document
from mail.payments.payments.core.entities.enums import DocumentType


@pytest.fixture
def db_engine(mocked_db_engine):
    return mocked_db_engine


@pytest.fixture
async def db_conn(loop, db_engine):
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
def base_tvm(mocker):
    mock = mocker.patch('sendr_tvm.qloud_async_tvm.TicketCheckResult')
    return mock


@pytest.fixture
def tvm_client_id(service_client):
    return service_client.tvm_id


@pytest.fixture
def merchant_documents(randn):
    return [
        Document(
            document_type=DocumentType.OFFER,
            path='test-document-entity-offer-path',
            size=randn(),
            name='test-document-entity-offer',
        ),
        Document(
            document_type=DocumentType.PASSPORT,
            path='test-document-entity-passport-path',
            size=randn(),
            name='test-document-entity-passport',
        ),
        Document(
            document_type=DocumentType.PASSPORT,
            path='test-document-entity-passport-other-path',
            size=randn(),
            name='test-document-entity-passport-other',
        ),
    ]


@pytest.fixture
def document_entity(merchant_documents):
    return merchant_documents[0]


@pytest.fixture
def path(document_entity):
    return document_entity.path
