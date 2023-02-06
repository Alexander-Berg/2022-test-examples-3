import tarfile

import mock
import pytest

from crypta.dmp.yandex.bin.ftp_to_yt.lib import (
    exceptions,
    helpers
)


class TarFileMock(object):
    def __init__(self, extractall_exception):
        self.extractall_exception = extractall_exception

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        pass

    def extractall(self, path):
        raise self.extractall_exception


@pytest.mark.parametrize("extractall_exception,expected_exception,expected_message", [
    (tarfile.ReadError("not a gzip file"), exceptions.DmpArchiveError, "invalid archive format"),
    (IOError("CRC check failed 0xaea1275f != 0x8db4470dL"), exceptions.DmpArchiveError, "invalid archive format. CRC check failed"),
    (IOError("disk full"), IOError, "disk full")
], ids=[
    "ReadError: not a gzip file",
    "IOError: CRC check failed",
    "IOError: disk full"
])
def test_extract_negative(extractall_exception, expected_exception, expected_message):
    with pytest.raises(expected_exception, match=expected_message):
        with mock.patch("crypta.dmp.yandex.bin.ftp_to_yt.lib.helpers.tarfile.open", lambda path, mode: TarFileMock(extractall_exception)):
            helpers.extract("segments-1547413200.tar.gz", "/tmp")
