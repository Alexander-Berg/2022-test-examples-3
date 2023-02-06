import pytest
import io

from hamcrest import assert_that, equal_to

from unistat.readers import (
    LineReader,
    BufferedLineReader,
    PaReader,
    PaRecord,
)


@pytest.mark.parametrize(('text', 'expected'), (
    ('', []),
    ('foo\n', [u'foo']),
    ('foo\nbar', [u'foo']),
    ('foo\nbar\n', [u'foo']),
))
def test_unbuffered_line_reader_single_call(text, expected):
    reader = LineReader()
    fd = io.BytesIO(text)
    assert_that(list(reader(fd)), equal_to(expected))


@pytest.mark.parametrize(('text', 'expected'), (
    ('', []),
    ('foo\n', [u'foo']),
    ('foo\nbar', [u'foo']),
    ('foo\nbar\n', [u'foo', u'bar']),
))
def test_unbuffered_line_reader_call_until_eof(text, expected):
    reader = LineReader()
    fd = io.BytesIO(text)
    result = read_until_eof(reader, fd)
    assert_that(result, equal_to(expected))


@pytest.mark.parametrize(('text', 'expected'), (
    ('', []),
    ('foo\n', [u'foo']),
    ('foo\nbar', [u'foo']),
    ('foo\nbar\n', [u'foo', u'bar']),
    (
        'a' * (BufferedLineReader.DEFAULT_BUFFER_SIZE - 1) + '\n',
        [u'a' * (BufferedLineReader.DEFAULT_BUFFER_SIZE - 1)]
    ),
    (
        'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE + '\n',
        []
    ),
    (
        'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE * 2 + '\n',
        []
    ),
))
def test_buffered_line_reader_single_call(text, expected):
    reader = BufferedLineReader()
    fd = io.BytesIO(text)
    assert_that(list(reader(fd)), equal_to(expected))


@pytest.mark.parametrize(('text', 'expected'), (
    ('', []),
    ('foo\n', [u'foo']),
    ('foo\nbar', [u'foo']),
    ('foo\nbar\n', [u'foo', u'bar']),
    (
        'a' * (BufferedLineReader.DEFAULT_BUFFER_SIZE - 1) + '\n',
        [u'a' * (BufferedLineReader.DEFAULT_BUFFER_SIZE - 1)]
    ),
    (
        'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE + '\n',
        [u'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE]
    ),
    (
        'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE * 2 + '\n',
        [u'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE * 2]
    ),
    (
        'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE * 3 + '\n',
        [u'a' * BufferedLineReader.DEFAULT_BUFFER_SIZE * 3]
    ),
    (
        'a' * (BufferedLineReader.DEFAULT_BUFFER_SIZE - 2) + '\nbb\n',
        [u'a' * (BufferedLineReader.DEFAULT_BUFFER_SIZE - 2), u'bb']
    ),
))
def test_buffered_line_reader_call_until_eof(text, expected):
    reader = BufferedLineReader()
    fd = io.BytesIO(text)
    result = read_until_eof(reader, fd)
    assert_that(result, equal_to(expected))


@pytest.mark.parametrize(('text', 'expected'), (
    ('', []),
    ('\x00' * 67, []),
    ('\x00' * 68, [PaRecord(type=0, host='', req='', suid='', spent_ms=0, timestamp=0)]),
    ('\x00' * 69, [PaRecord(type=0, host='', req='', suid='', spent_ms=0, timestamp=0)]),
    ('\x00' * 68 * 2, [PaRecord(type=0, host='', req='', suid='', spent_ms=0, timestamp=0)]),
))
def test_pa_reader_single_call(text, expected):
    reader = PaReader()
    fd = io.BytesIO(text)
    assert_that(list(reader(fd)), equal_to(expected))


@pytest.mark.parametrize(('text', 'expected'), (
    ('', []),
    ('\x00' * 67, []),
    ('\x00' * 68, [PaRecord(type=0, host='', req='', suid='', spent_ms=0, timestamp=0)]),
    ('\x00' * 69, [PaRecord(type=0, host='', req='', suid='', spent_ms=0, timestamp=0)]),
    ('\x00' * 68 * 2, [PaRecord(type=0, host='', req='', suid='', spent_ms=0, timestamp=0)] * 2),
))
def test_pa_reader_call_until_eof(text, expected):
    reader = PaReader()
    fd = io.BytesIO(text)
    result = read_until_eof(reader, fd)
    assert_that(result, equal_to(expected))


def read_until_eof(reader, fd):
    result = list()
    while not reader.eof():
        result += list(reader(fd))
    return result
