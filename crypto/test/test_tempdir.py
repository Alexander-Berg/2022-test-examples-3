from __future__ import absolute_import

import mock
import pytest

from crypta.lib.python.yt.yt_helpers import tempdir


UUID4 = "56c3447e-5634-4446-b655-2d7919f3569e"
TIMESTAMP = 1514263720.434435


class MockYtClient(object):
    def __init__(self, *args, **kwargs):
        self.removed = []

    def remove(self, path, **kwargs):
        self.removed.append(path)

    def exists(self, *args, **kwargs):
        return False

    def create(self, *args, **kwargs):
        pass

    class Transaction(object):
        def __init__(self):
            self.transaction_id = "tx_id"

        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc_val, exc_tb):
            pass


@pytest.mark.parametrize("input_dir,ref_dir", [
    ("path/1", "//path/1"),
    ("//path/2", "//path/2"),
])
def test_to_absolute_path(input_dir, ref_dir):
    assert tempdir.to_absolute_path(input_dir) == ref_dir


def generate_subdir(dir):
    return "{}/{}-{}".format(dir, UUID4, int(TIMESTAMP))


def generate_test_params(dir):
    subdir = generate_subdir(dir)
    return dir, subdir, [tempdir.to_absolute_path(subdir)]


@pytest.mark.parametrize("input_tmp,ref_tmp,ref_removed", [
    generate_test_params("path/to/tmp"),
    generate_test_params("//path/to/tmp"),
])
def test_tempdir(input_tmp, ref_tmp, ref_removed):
    yt_client = MockYtClient()
    with mock.patch("crypta.lib.python.yt.yt_helpers.tempdir.uuid.uuid4", return_value=UUID4), \
            mock.patch("crypta.lib.python.yt.yt_helpers.tempdir.time.time", return_value=TIMESTAMP), \
            tempdir.YtTempDir(yt_client, input_tmp) as res:
        assert ref_tmp == res.path

    assert ref_removed == yt_client.removed
