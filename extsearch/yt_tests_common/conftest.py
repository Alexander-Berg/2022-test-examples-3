import pytest
import yt.wrapper as yt


@pytest.yield_fixture
def yt_test_dir():
    test_dir = '//tmp/test_dir'
    if yt.exists(test_dir):
        yt.remove(test_dir, recursive=True)
    yt.create('map_node', test_dir)
    yield test_dir
    yt.remove(test_dir, recursive=True)


@pytest.yield_fixture
def yt_test_table(yt_test_dir):
    with yt.TempTable(path=yt_test_dir) as tmp_table:
        yield tmp_table
