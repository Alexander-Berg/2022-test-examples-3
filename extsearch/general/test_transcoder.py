import logging
# import yatest.common
# import pytest

from extsearch.video.contstorage.protos.content_pb2 import EContType
from extsearch.video.robot.speech2text.transcoder.lib import (
    cut_scheme,
    empty_signature_row,
    signature_stub
)

logger = logging.getLogger("test_logger")


def test_cut_url():
    assert cut_scheme('http://ya.ru') == 'ya.ru'
    assert cut_scheme('https://ya.ru') == 'ya.ru'
    assert cut_scheme('ftp://ya.ru') == 'ya.ru'
    assert cut_scheme('HtTp://ya.ru') == 'ya.ru'
    assert cut_scheme('//ya.ru') == 'ya.ru'
    assert cut_scheme('ya.ru') == 'ya.ru'
    assert cut_scheme('s3://ya.ru') == 'ya.ru'


def test_empty_signature():
    row = empty_signature_row('http://ya.ru', 1000, 2)
    assert row[b'md5'] == b'3fbdd5192c1ab47c3819aae644c9aee5'
    assert row[b'signature'] == b'IgQYASAB'
    assert row[b'url'] == b'ya.ru'
    row = empty_signature_row(b'http://ya.ru', 1000, 2)
    assert row[b'url'] == b'ya.ru'


def test_content_signature_stub():
    signature_stub('http://ya.ru', 1000, 100000)
    assert True


def test_misc():
    assert EContType.Value(b'EAudio') == 2
    assert EContType.Value('EAudio') == 2
    assert EContType.Value(b'EAudio2') == 4
    assert EContType.Value('EAudio2') == 4
