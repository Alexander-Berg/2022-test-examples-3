import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.csvops.linereader import CSVLineReader, Line
from mail.ipa.ipa.core.exceptions import CSVEmptyError, CSVEncodingDetectError, CSVIsTooBig, CSVLineIsTooBig


async def consume_gen(gen):
    return [line async for line in gen]


class TestReadLines:
    @pytest.fixture
    async def readlines(self, ipa_settings, data_chunks):
        async def data_gen():
            for chunk in data_chunks:
                yield chunk

        reader = CSVLineReader(data_gen())

        return reader.readlines()

    class TestCSVWithOneBigLine:
        @pytest.fixture
        def data_chunks(self):
            return [b'0' * 2048 + b'\n', b'123', b'123']

        @pytest.fixture(autouse=True)
        def buffer_limit(self):
            CSVLineReader.LINE_LENGTH_LIMIT = 1024

        @pytest.mark.asyncio
        async def test_big_line_raises_csv_too_big(self, readlines):
            with pytest.raises(CSVLineIsTooBig):
                await consume_gen(readlines)

    class TestLinesAreCorrect:
        @pytest.fixture
        def data_chunks(self):
            return [b'123\n456', b'789\n', b'\n \n', b'012']

        @pytest.mark.asyncio
        async def test_lines_are_correct(self, readlines):
            assert [
                Line(b'123\n', 1),
                Line(b'456789\n', 2),
                Line(b'012', 5)
            ] == await consume_gen(readlines)

    @pytest.mark.parametrize('data_chunks', (
        (b'',), (b'   ',), (b'\n\n\n',))
    )
    @pytest.mark.asyncio
    async def test_empty(self, readlines):
        with pytest.raises(CSVEmptyError):
            await consume_gen(readlines)

    class TestCSVTooBig:
        @pytest.fixture(autouse=True)
        def csv_size_limit(self, ipa_settings):
            ipa_settings.CSV_MAX_SIZE = 1024

        @pytest.mark.parametrize('data_chunks', (
            (b'0' * 100 + b'\n',) * 20,)
        )
        @pytest.mark.asyncio
        async def test_too_big(self, readlines):
            with pytest.raises(CSVIsTooBig):
                await consume_gen(readlines)


class BaseTestDetectCSVParams:
    @pytest.fixture(autouse=True)
    def sample_limit(self):
        CSVLineReader.SAMPLE_LIMIT = 1000

    @pytest.fixture
    def data(self, sample, encoding):
        return sample.encode(encoding)

    @pytest.fixture
    def probe(self, data):
        async def data_chunks():
            yield data

        reader = CSVLineReader(data_chunks())
        return reader.detect_csv_params()

    @pytest.fixture
    async def returned(self, probe):
        return await probe

    @pytest.fixture(autouse=True)
    def mock_sniff(self, mocker):
        return mocker.patch('mail.ipa.ipa.core.csvops.linereader.sniff', mocker.Mock())


@pytest.mark.parametrize('sample, encoding', (
    ('привет,мир', 'utf-8'),
    ('привет,мир', 'cp1251'),
    ('привет,мир', 'koi8-r'),
    ('header1,header2', 'ascii'),
    (""" yandexmail_login,src_password,src_login,yandexmail_password,name,surname,middlename,gender,birthday,language
inpfile02goog,*******,testsarahsbor01@gmail.com,*******,Иван,Иванов,Иванович,male,01.02.2001,ru
inpfile03goog,*******,calassessors@gmail.com,*******,Иван2,Иванов2,Иванович2,female,01.02.2001,en
inpfile04goog,*******,testsarahsbor02@gmail.com,*******,Иван3,Иванов3,Иванович3,female,01.02.2001,ru""", 'cp1251'),
))
class TestDetectCSVParams(BaseTestDetectCSVParams):
    def test_encoding(self, returned, sample, data):
        assert_that(data.decode(returned.encoding), equal_to(sample))

    def test_dialect(self, mock_sniff, returned, sample):
        assert_that(returned.dialect, equal_to(mock_sniff()))


@pytest.mark.parametrize('sample, encoding', (
    ("""yandexmail_login,src_password,src_login,yandexmail_password,name,surname,middlename,gender,birthday,language
inpfile01,********,testsarahsbor01@gmail.com,*******,Василий,Васильевич,Васильев,male,01.01.2001,ru""", 'cp1251'),
))
class TestDetectCSVParamsEncodingException(BaseTestDetectCSVParams):
    @pytest.mark.asyncio
    async def test_raises(self, probe):
        with pytest.raises(CSVEncodingDetectError):
            await probe
