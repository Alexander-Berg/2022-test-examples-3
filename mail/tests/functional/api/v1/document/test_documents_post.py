import aiohttp
import pytest
from library.python import resource

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.entities.document import DocumentType
from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


class TestDocumentsPost(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def mds_path(self):
        return 'some/path'

    @pytest.fixture(autouse=True)
    def mds_upload_mock(self, mds_client_mocker, mds_path):
        with mds_client_mocker('upload', mds_path) as mock:
            yield mock

    @pytest.fixture(params=['image.png', 'image.jpg'])
    def file_name(self, request):
        return request.param

    @pytest.fixture
    def file_path(self, file_name):
        return 'resfs/file/tests/functional/api/v1/document/files/' + file_name

    @pytest.fixture
    def file_bytes(self, file_path):
        return resource.find(file_path)

    @pytest.fixture
    def file_size(self, file_bytes):
        return len(file_bytes)

    @pytest.fixture(params=list(DocumentType))
    def document_type(self, request):
        return request.param

    @pytest.fixture
    async def mpwriter(self, file_bytes, document_type, file_name):
        with aiohttp.MultipartWriter() as mpwriter:
            part = mpwriter.append(file_bytes)
            part.set_content_disposition('attachment', name=document_type.value, filename=file_name)
            return mpwriter

    @pytest.fixture
    async def document_response(self, client, merchant, mpwriter, tvm):
        response = await client.post(f'/v1/document/{merchant.uid}', data=mpwriter)
        return (await response.json())['data']

    @pytest.fixture
    async def updated_merchant(self, storage, merchant, document_response):
        return await storage.merchant.get(merchant.uid)

    def test_response(self, mds_path, file_size, document_type, document_response, file_name):
        assert_that(
            document_response,
            has_entries({
                'path': mds_path,
                'size': file_size,
                'document_type': document_type.value,
                'name': file_name,
            }),
        )

    def test_updated_merchant(self, mds_path, file_size, document_type, file_name, updated_merchant):
        assert_that(
            updated_merchant.documents[-1],
            has_properties({
                'path': mds_path,
                'size': file_size,
                'document_type': document_type,
                'name': file_name,
            })
        )

    class TestNotAllowedDocumentType:
        @pytest.fixture
        def file_name(self):
            return 'image.jpg'

        @pytest.fixture
        def document_type(self, mocker):
            mock = mocker.Mock()
            mock.value = 'test-not-allowed-document-type'
            return mock

        def test_returns_document_type_error(self, document_type, document_response):
            assert document_response == {
                'message': 'DOCUMENT_TYPE_NOT_ALLOWED'
            }

    class TestNotAllowedMimeType:
        @pytest.fixture
        def file_name(self):
            return 'animated_image.png'

        def test_returns_mimetype_error(self, document_response):
            assert document_response == {
                'message': 'DOCUMENT_FILE_TYPE_NOT_ALLOWED',
            }

    class TestFileSizeLimitExceeded:
        @pytest.fixture
        def file_name(self):
            return 'image.jpg'

        @pytest.fixture(autouse=True)
        def setup(self, payments_settings, file_size):
            payments_settings.DOCUMENT_MAX_SIZE = file_size - 1

        def test_returns_file_size_error(self, document_response):
            assert document_response == {
                'message': 'DOCUMENT_FILE_SIZE_LIMIT_EXCEEDED'
            }
