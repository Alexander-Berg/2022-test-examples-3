import csv

import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.csvops.base import CSVDownloadHelper, CSVOperations, CSVUploadHelper
from mail.ipa.ipa.core.csvops.linereader import Line
from mail.ipa.ipa.core.entities.csv import CSVParams
from mail.ipa.ipa.core.exceptions import (
    CSVEmptyError, CSVEncodingDetectError, CSVHeaderFieldDuplicateError, CSVHeaderFieldRequiredError,
    CSVHeaderFieldUnknownError, CSVMalformedError
)


class LineReaderMock:
    def __init__(self, data, enc, dialect):
        self.data = data.splitlines(keepends=True)
        self.data.reverse()
        self.enc = enc
        self.dialect = dialect

    async def detect_csv_params(self) -> CSVParams:
        return CSVParams(self.enc, self.dialect)

    async def readlines(self):
        while len(self.data):
            line = self.data.pop()
            yield Line(line, 1)


@pytest.fixture
def ops():
    return CSVOperations()


@pytest.fixture
def ipa_settings(ipa_settings):
    ipa_settings.CSV_REQUIRED_HEADERS = ['header1', 'header2']
    ipa_settings.CSV_OPTIONAL_HEADERS = ['header3', 'header4']
    ipa_settings.CSV_MAX_SIZE = 1024 * 512
    return ipa_settings


class TestValidateCSV:
    REGULAR_CASES = (
        (b'header1,header2\nfoo,bar', b'header1,header2\nfoo,bar'),
        (b'header1,header2\n,\nfoo,bar', b'header1,header2\nfoo,bar'),
    )

    @pytest.fixture
    def encoding(self):
        return 'utf8'

    @pytest.fixture
    def validate_cb(self):
        return None

    @pytest.fixture
    def validate_csv_gen(self, data, encoding, ipa_settings, validate_cb):
        uploader = CSVUploadHelper(validate_cb=validate_cb)
        return uploader.validate(LineReaderMock(data, encoding, csv.unix_dialect))

    @pytest.fixture
    async def returned(self, validate_csv_gen):
        return b''.join([line async for line in validate_csv_gen])

    @pytest.mark.parametrize('data, expected', (
        (b'header1,header2\nfoo,bar', b'header1,header2\nfoo,bar'),
        (b'header1,header2\n,\nfoo,bar', b'header1,header2\nfoo,bar'),
    ))
    def test_returned(self, returned, expected):
        assert_that(returned, equal_to(expected))

    class TestCallsValidateCallback:
        @pytest.fixture
        def validate_cb(self, mocker):
            return mocker.Mock()

        @pytest.mark.parametrize('data, entries', (
            (b'header1,header2\nfoo,bar', [{'header1': 'foo', 'header2': 'bar'}]),
        ))
        def test_calls_validate_cb(self, mocker, entries, returned, validate_cb):
            validate_cb.assert_has_calls([mocker.call(entry, idx + 1) for idx, entry in enumerate(entries)])

    class TestError:
        @pytest.mark.parametrize('data', (b'header1\nfoo\nbar',))
        @pytest.mark.asyncio
        async def test_raises_csv_invalid_header(self, validate_csv_gen):
            with pytest.raises(CSVHeaderFieldRequiredError):
                await alist(validate_csv_gen)

        @pytest.mark.parametrize('data', (
            b'header1,header2\nfoo1,foo2\nbar\nbaz1,baz2',
            b'header1,header2\n,,\nbaz1,baz2',
        ))
        @pytest.mark.asyncio
        async def test_raises_csv_malformed_column(self, validate_csv_gen):
            with pytest.raises(CSVMalformedError):
                await alist(validate_csv_gen)

        @pytest.mark.parametrize('data', (b'header1,header2\n',))
        @pytest.mark.asyncio
        async def test_empty_csv(self, validate_csv_gen, data):
            with pytest.raises(CSVEmptyError):
                await alist(validate_csv_gen)

        @pytest.mark.asyncio
        @pytest.mark.parametrize('data', (
            'привет,мир'.encode('cp1251'),
            b'header1,header2' + 'привет,мир'.encode('cp1251'),
        ))
        async def test_ambigous_encoding(self, validate_csv_gen):
            with pytest.raises(CSVEncodingDetectError):
                await alist(validate_csv_gen)


class TestEncryptCSV:
    @pytest.fixture
    def expected_chunks(self):
        return [b'123', b'456', b'789']

    @pytest.fixture(autouse=True)
    def mock_encryptor(self, mocker, expected_chunks):
        return mocker.patch('mail.ipa.ipa.core.crypto.BlockEncryptor.update',
                            mocker.Mock(side_effect=expected_chunks))

    @pytest.fixture(autouse=True)
    def mock_encryptor_finalize(self, mocker, expected_chunks):
        return mocker.patch('mail.ipa.ipa.core.crypto.BlockEncryptor.finalize',
                            mocker.Mock(return_value=b''))

    @pytest.fixture
    def input_chunks(self):
        return (bytes([1] * 16), bytes([1] * 15), bytes([1] * 3))

    @pytest.fixture
    def expected_calls_to_encryptor(self, input_chunks):
        return input_chunks

    @pytest.fixture
    def encrypt_csv_gen(self, input_chunks):
        async def input_chunks_gen():
            for chunk in input_chunks:
                yield chunk

        uploader = CSVUploadHelper()
        return uploader.encrypt(input_chunks_gen())

    @pytest.fixture(autouse=True)
    async def returned(self, encrypt_csv_gen):
        return b''.join(await alist(encrypt_csv_gen))

    def test_encrypt_gen(self, returned, expected_chunks):
        cyphertext = returned
        assert_that(
            cyphertext,
            equal_to(b''.join(expected_chunks)),
        )

    def test_calls_encryptor_update(self, mocker, mock_encryptor, expected_calls_to_encryptor):
        expected_calls = [mocker.call(chunk) for chunk in expected_calls_to_encryptor]
        mock_encryptor.assert_has_calls(expected_calls)

    def test_calls_encryptor(self, mocker, mock_encryptor_finalize, expected_calls_to_encryptor):
        mock_encryptor_finalize.assert_called_once_with()


class TestDecryptCSV:
    @pytest.fixture(autouse=True)
    def mock_decryptor(self, mocker):
        return mocker.patch('mail.ipa.ipa.core.crypto.BlockDecryptor.update',
                            mocker.Mock(side_effect=[b'plaintext']))

    @pytest.fixture(autouse=True)
    def mock_decryptor_finalize(self, mocker):
        return mocker.patch('mail.ipa.ipa.core.crypto.BlockDecryptor.finalize', mocker.Mock(return_value=b'decrypted'))

    @pytest.fixture
    def decrypt_csv_gen(self):
        async def iter_chunks():
            yield b'cyphertext'

        downloader = CSVDownloadHelper()
        return downloader.decrypt(iter_chunks())

    @pytest.fixture
    async def returned(self, decrypt_csv_gen):
        return b''.join(await alist(decrypt_csv_gen))

    def test_returned(self, returned):
        assert_that(
            returned,
            equal_to(b'plaintextdecrypted'),
        )

    def test_decryptor_called(self, returned, mock_decryptor):
        mock_decryptor.assert_called_once_with(b'cyphertext')

    def test_decryptor_finalize_called(self, returned, mock_decryptor_finalize):
        mock_decryptor_finalize.assert_called_once_with()


class TestGetValidatedHeader:
    @pytest.fixture
    def csv_params(self, mocker):
        params = mocker.Mock()
        params.encoding = 'ascii'
        params.dialect = csv.excel
        return params

    @pytest.fixture
    def get_validated_header(self, ops, ipa_settings, csv_params):
        def _inner(data):
            ops.get_validated_header(Line(data, 1), csv_params)

        return _inner

    def test_returned(self, get_validated_header):
        get_validated_header(b'header1,header2,header4')

    def test_unknown_header(self, get_validated_header):
        with pytest.raises(CSVHeaderFieldUnknownError):
            get_validated_header(b'header5')

    @pytest.mark.parametrize('data', (b'header1,header2,header1,header3,header4', b'header1,header2,header3,header3'))
    def test_duplicate_header(self, get_validated_header, data):
        with pytest.raises(CSVHeaderFieldDuplicateError):
            get_validated_header(data)

    def test_required_header(self, get_validated_header):
        with pytest.raises(CSVHeaderFieldRequiredError):
            get_validated_header(b'header1,header3,header4')


class TestReadCSV:
    @pytest.fixture
    def read_csv_gen(self):
        async def input_chunks_gen():
            yield b'header1,header2\n'
            yield b'abc,def\n'
            yield b'aaa,bbb'

        downloader = CSVDownloadHelper()
        return downloader.read(
            LineReaderMock(b'header1,header2\nabc,def\naaa,bbb', 'utf-8', csv.excel)
        )

    @pytest.fixture
    async def returned(self, read_csv_gen):
        return await alist(read_csv_gen)

    def test_returned(self, returned):
        assert_that(
            returned,
            equal_to(
                [
                    {'header1': 'abc', 'header2': 'def'},
                    {'header1': 'aaa', 'header2': 'bbb'},
                ]
            )
        )


class TestChunkMultiplexer:
    @pytest.fixture
    def data(self):
        return [b'a' * 50000, b'b' * 50000, b'c' * 50000, b'd' * 50000]

    @pytest.fixture
    def chunk_multiplexer_gen(self, data):
        async def input_chunks_gen():
            for d in data:
                yield d

        uploader = CSVUploadHelper()
        return uploader.chunk_multiplexer(input_chunks_gen())

    @pytest.fixture
    async def returned(self, chunk_multiplexer_gen):
        return await alist(chunk_multiplexer_gen)

    def test_returned_is_correct(self, returned, data):
        assert_that(b''.join(returned), equal_to(b''.join(data)))

    def test_chunks_are_big(self, returned):
        for i in range(len(returned) - 1):
            assert_that(len(returned[i]) >= 65536)
