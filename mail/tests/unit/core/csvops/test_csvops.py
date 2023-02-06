import pytest

from mail.ipa.ipa.core.csvops import prepare_csv_for_upload, read_downloaded_csv
from mail.ipa.ipa.core.csvops.base import CSVDownloadHelper, CSVUploadHelper
from mail.ipa.ipa.core.csvops.linereader import CSVLineReader


class TestUploadHelper:
    @pytest.fixture
    def validate_cb(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def stream(self, mocker):
        mock = mocker.Mock()

        class StreamReaderMock:
            def iter_chunked(self, *args, **kwargs):
                return mock

        return StreamReaderMock()

    @pytest.fixture(autouse=True)
    def mock_chunk_multiplexer(self, mocker):
        return mocker.patch.object(CSVUploadHelper, 'chunk_multiplexer', mocker.Mock())

    @pytest.fixture(autouse=True)
    def mock_encrypt(self, mocker):
        return mocker.patch.object(CSVUploadHelper, 'encrypt', mocker.Mock())

    @pytest.fixture(autouse=True)
    def mock_validate(self, mocker):
        return mocker.patch.object(CSVUploadHelper, 'validate', mocker.Mock())

    @pytest.fixture(autouse=True)
    def mock_readlines(self, mocker):
        return mocker.patch.object(CSVLineReader, '__init__', mocker.Mock(return_value=None))

    @pytest.fixture(autouse=True)
    async def returned(self, stream, validate_cb):
        return prepare_csv_for_upload(stream, validate_cb=validate_cb)

    def test_calls_readlines(self, stream, mock_readlines):
        mock_readlines.assert_called_once_with(stream.iter_chunked())

    def test_calls_validate(self, mock_validate):
        mock_validate.assert_called_once()
        assert isinstance(mock_validate.call_args[0][0], CSVLineReader)

    def test_calls_encrypt(self, mock_validate, mock_encrypt):
        mock_encrypt.assert_called_once_with(mock_validate())

    def test_calls_multiplexer(self, mock_chunk_multiplexer, mock_encrypt):
        mock_chunk_multiplexer.assert_called_once_with(mock_encrypt())

    def test_returned(self, returned, mock_chunk_multiplexer):
        assert returned == mock_chunk_multiplexer()


class TestDownloadHelper:
    @pytest.fixture
    def stream(self, mocker):
        mock = mocker.Mock()

        class StreamReaderMock:
            def iter_chunked(self, *args, **kwargs):
                return mock

        return StreamReaderMock()

    @pytest.fixture(autouse=True)
    def mock_decrypt(self, mocker):
        return mocker.patch.object(CSVDownloadHelper, 'decrypt', mocker.Mock())

    @pytest.fixture(autouse=True)
    def mock_read(self, mocker):
        return mocker.patch.object(CSVDownloadHelper, 'read', mocker.Mock())

    @pytest.fixture(autouse=True)
    def mock_readlines(self, mocker):
        return mocker.patch.object(CSVLineReader, '__init__', mocker.Mock(return_value=None))

    @pytest.fixture(autouse=True)
    async def returned(self, stream):
        return read_downloaded_csv(stream)

    def test_calls_decrypt(self, mock_decrypt, stream):
        mock_decrypt.assert_called_once_with(stream.iter_chunked())

    def test_calls_readlines(self, mock_decrypt, mock_readlines):
        mock_readlines.assert_called_once_with(mock_decrypt())

    def test_calls_read(self, mock_read):
        mock_read.assert_called_once()
        assert isinstance(mock_read.call_args[0][0], CSVLineReader)
