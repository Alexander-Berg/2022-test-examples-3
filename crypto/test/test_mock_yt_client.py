import pytest

import yt.wrapper as yt

from crypta.spine.pushers.common import mocks


MAP_NODE_TYPE = "map_node"
TIME_06_02 = "2021-06-02T03:00:00.000000Z"

nodes = [
    mocks.Node("empty-dir", TIME_06_02, type=MAP_NODE_TYPE),
    mocks.Node("one-table-dir/2021-06-02", TIME_06_02),

    mocks.Node("complex-dir/2021-06-02", TIME_06_02),
    mocks.Node("complex-dir/subdir", TIME_06_02, type="map_node"),
    mocks.Node("complex-dir/subdir/2021-06-04", TIME_06_02),
]


@pytest.fixture(scope="function")
def mock_yt_client():
    return mocks.MockYtClient(nodes)


def test_get_attributes(mock_yt_client):
    assert MAP_NODE_TYPE == mock_yt_client.get("empty-dir/@type")
    assert TIME_06_02 == mock_yt_client.get("empty-dir/@creation_time")

    ref_result = mocks.ResultOfYtGet(attributes={
        "creation_time": TIME_06_02,
        "type": MAP_NODE_TYPE,
    })
    assert ref_result == mock_yt_client.get("empty-dir", attributes=["creation_time", "type"])

    with pytest.raises(Exception):
        mock_yt_client.get("empty-dir")


def test_search(mock_yt_client):
    assert [yt.YPath("empty-dir", client=mock_yt_client)] == mock_yt_client.search("empty-dir", {}, lambda x: True)
    assert [] == mock_yt_client.search("empty-dir", [], lambda x: x != "empty-dir")

    assert [
        yt.YPath("one-table-dir/2021-06-02", attributes={"creation_time": TIME_06_02}, client=mock_yt_client)
    ] == mock_yt_client.search("one-table-dir", ["creation_time"], lambda x: x != "one-table-dir")


def test_exists(mock_yt_client):
    assert mock_yt_client.exists("empty-dir")
    assert not mock_yt_client.exists("foo")

    assert mock_yt_client.exists("one-table-dir")
    assert mock_yt_client.exists("one-table-dir/2021-06-02")

    assert mock_yt_client.exists("complex-dir")
    assert mock_yt_client.exists("complex-dir/subdir")
    assert mock_yt_client.exists("complex-dir/subdir/2021-06-04")
