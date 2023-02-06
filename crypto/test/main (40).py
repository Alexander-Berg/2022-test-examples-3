import pytest

from crypta.lib.python.data_size import DataSize


@pytest.mark.parametrize("data_size,ref_total_bytes", [
    (DataSize(), 0),
    (DataSize(b=1), 1),
    (DataSize(kb=1), 1024),
    (DataSize(mb=1), 1024 ** 2),
    (DataSize(gb=1), 1024 ** 3),
    (DataSize(tb=1), 1024 ** 4),
    (DataSize(tb=6, gb=7, mb=8, kb=9, b=10), 6604594357258),
])
def test_total_bytes(data_size, ref_total_bytes):
    assert ref_total_bytes == data_size.total_bytes()
