import pytest

from mail.payments.payments.core.actions.document import DownloadDocumentAction
from mail.payments.payments.core.entities.document import Document, DocumentType
from mail.payments.payments.core.exceptions import DocumentNotFoundError


class TestDownloadDocument:
    @pytest.fixture
    def path(self):
        return 'test-download-document-path'

    @pytest.fixture
    def content_type(self):
        return 'test-download-document-content-type'

    @pytest.fixture
    def data(self):
        return 'test-download-document-data'

    @pytest.fixture(autouse=True)
    def download_mock(self, mds_client_mocker, content_type, data):
        with mds_client_mocker('download', (content_type, data)) as mock:
            yield mock

    @pytest.fixture
    def merchant_documents(self, path):
        return [
            Document(
                document_type=DocumentType.PASSPORT,
                path=path,
                size=1234,
                name='test-download-document',
            ),
            Document(
                document_type=DocumentType.PASSPORT,
                path='test-download-document-other-path',
                size=5678,
                name='test-download-document-other',
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
    async def returned(self, params):
        return await DownloadDocumentAction(**params).run()

    @pytest.mark.asyncio
    async def test_raises_not_found(self, params):
        params['path'] = 'test-download-document-not-found-path'
        with pytest.raises(DocumentNotFoundError):
            await DownloadDocumentAction(**params).run()

    def test_download_call(self, path, download_mock, returned):
        download_mock.assert_called_once_with(path, close=True)

    class TestHeaders:
        @pytest.fixture(params=(
            'image/png',
            'image/jpeg',
        ))
        def content_type(self, request):
            return request.param

        @pytest.fixture
        def extension(self, content_type):
            tp = content_type.split('/')[1]
            return {
                'png': '.png',
                'jpeg': '.jpg',
            }.get(tp, '')

        def test_headers(self, content_type, extension, returned):
            assert returned[0] == {
                'Content-Type': content_type,
                'Content-Disposition': f'attachment; filename="document{extension}"',
            }
