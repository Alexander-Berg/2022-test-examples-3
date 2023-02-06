import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.document import UploadDocumentAction
from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.exceptions import (
    DocumentBodyPartError, DocumentCannotLoadImageError, DocumentFileSizeLimitExceededError,
    DocumentFileTypeNotAllowedError, DocumentRequestBodyEmptyError, DocumentTypeNotAllowedError
)
from mail.payments.payments.tests.base import BaseTestRequiresNoModeration
from mail.payments.payments.tests.utils import dummy_coro


class TestGetBodyPart:
    @pytest.fixture
    def part(self, mocker):
        mock = mocker.Mock()
        mock.name = DocumentType.PASSPORT.value
        return mock

    @pytest.fixture
    def reader(self, mocker, part):
        coro = dummy_coro(part)
        mock = mocker.Mock()
        mock.next.return_value = coro
        yield mock
        coro.close()

    @pytest.mark.asyncio
    async def test_returns_part(self, reader, part):
        assert await UploadDocumentAction._get_body_part(reader) == part

    @pytest.mark.asyncio
    async def test_raises_on_bad_name(self, reader, part):
        part.name = 'test-get-body-part-bad-name'
        with pytest.raises(DocumentTypeNotAllowedError):
            await UploadDocumentAction._get_body_part(reader)

    @pytest.mark.asyncio
    async def test_raises_on_empty(self, reader):
        reader.next.return_value = dummy_coro(None)
        with pytest.raises(DocumentRequestBodyEmptyError):
            await UploadDocumentAction._get_body_part(reader)

    @pytest.mark.asyncio
    async def test_get_body_part_error(self, reader):
        reader.next.return_value = dummy_coro(exc=ValueError)
        with pytest.raises(DocumentBodyPartError):
            await UploadDocumentAction._get_body_part(reader)


class TestReadBodyPart:
    @pytest.fixture
    def max_size(self):
        return 100

    @pytest.fixture
    def chunks(self):
        return [b'aaa', b'bbb', b'ccc']

    @pytest.fixture(autouse=True)
    def setup(self, payments_settings, max_size):
        payments_settings.DOCUMENT_MAX_SIZE = max_size

    @pytest.fixture
    def part(self, mocker, chunks):
        mock = mocker.Mock()
        mock.read_chunk_calls = 0
        chunks_iter = iter(chunks)

        async def _read_chunk():
            nonlocal mock, chunks_iter
            mock.read_chunk_calls += 1
            return next(chunks_iter, None)

        mock.read_chunk = _read_chunk
        return mock

    @pytest.mark.asyncio
    async def test_returned(self, chunks, part):
        joined_chunks = b''.join(chunks)
        data, size = await UploadDocumentAction._read_body_part(part)
        assert data == joined_chunks and size == len(joined_chunks)

    @pytest.mark.asyncio
    async def test_read_calls(self, chunks, part):
        await UploadDocumentAction._read_body_part(part)
        assert part.read_chunk_calls == len(chunks) + 1

    class TestFileSizeLimit:
        @pytest.fixture
        def chunks(self):
            return [
                b'a' * 40
            ] * 3

        @pytest.mark.asyncio
        async def test_raises_data_error(self, part):
            with pytest.raises(DocumentFileSizeLimitExceededError):
                await UploadDocumentAction._read_body_part(part)


class TestValidateImage:
    @pytest.fixture
    def data(self):
        return b'test-validate-image-data'

    @pytest.fixture
    def mimetype(self):
        return 'test-validate-image-mimetype'

    @pytest.fixture
    def image(self, mocker, payments_settings, mimetype):
        payments_settings.ALLOWED_IMAGE_MIMETYPES = (mimetype,)
        mock = mocker.Mock()
        mock.get_format_mimetype.return_value = mimetype
        return mock

    @pytest.fixture(autouse=True)
    def open_mock(self, mocker, image):
        return mocker.patch(
            'mail.payments.payments.core.actions.document.Image.open',
            mocker.Mock(return_value=image),
        )

    def test_raises_on_open_fail(self, data, open_mock):
        open_mock.side_effect = IOError
        with pytest.raises(DocumentCannotLoadImageError):
            UploadDocumentAction.validate_image(data)

    def test_raises_on_wrong_mimetype(self, payments_settings, data, image):
        payments_settings.ALLOWED_IMAGE_MIMETYPES = ('a', 'b')
        image.get_format_mimetype.return_value = 'test-validate-image-wrong-mimetype'
        with pytest.raises(DocumentFileTypeNotAllowedError):
            UploadDocumentAction.validate_image(data)

    def test_passes(self, data, image):
        UploadDocumentAction.validate_image(data)


class TestUploadDocument(BaseTestRequiresNoModeration):
    @pytest.fixture
    def merchant_documents(self, randn):
        return [
            Document(
                document_type=DocumentType.PASSPORT,
                path='test-upload-document-existing-path',
                size=randn(),
                name='test-upload-document-existing',
            ),
        ]

    @pytest.fixture
    def document_type(self):
        return DocumentType.PASSPORT

    @pytest.fixture
    def data(self):
        return b'test-upload-document-data'

    @pytest.fixture
    def size(self, randn):
        return randn()

    @pytest.fixture
    def name(self):
        return 'test-upload-document'

    @pytest.fixture
    def path(self):
        return 'test-upload-document-path'

    @pytest.fixture(autouse=True)
    def read_mock(self, mocker, document_type, data, size, name):
        coro = dummy_coro((document_type, data, size, name))
        yield mocker.patch.object(UploadDocumentAction, 'read', mocker.Mock(return_value=coro))
        coro.close()

    @pytest.fixture(autouse=True)
    def validate_image_mock(self, mocker):
        return mocker.patch.object(UploadDocumentAction, 'validate_image', mocker.Mock())

    @pytest.fixture(autouse=True)
    def upload_mock(self, mds_client_mocker, path):
        with mds_client_mocker('upload', path) as mock:
            yield mock

    @pytest.fixture(params=('uid', 'merchant'))
    def params(self, request, merchant):
        data = {'uid': merchant.uid, 'merchant': merchant}
        return {
            'reader': None,
            request.param: data[request.param],
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await UploadDocumentAction(**params).run()
        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def updated_merchant(self, storage, merchant, returned):
        return await storage.merchant.get(merchant.uid)

    def test_returned(self, document_type, size, path, name, returned):
        assert_that(
            returned,
            has_properties({
                'document_type': document_type,
                'size': size,
                'path': path,
                'name': name,
            })
        )

    def test_updated_merchant(self, merchant_documents, returned, updated_merchant):
        assert updated_merchant.documents == merchant_documents + [returned]

    def test_read_call(self, read_mock, returned):
        read_mock.assert_called_once()

    def test_validate_image_call(self, data, validate_image_mock, returned):
        validate_image_mock.assert_called_once()

    def test_upload_call(self, merchant, data, upload_mock, returned):
        upload_mock.assert_called_once_with(f'uid_{merchant.uid}.document', data)
