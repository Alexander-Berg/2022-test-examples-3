import datetime

import mock
import pytest

from crypta.spine.pushers.common import mocks
from crypta.spine.pushers.yt_latencies import lib


@pytest.fixture
def mock_datetime():
    class MockDatetime(datetime.datetime):
        @classmethod
        def utcnow(cls):
            return datetime.datetime(2018, 10, 05, 12, 00, 00)

    return mock.patch("crypta.spine.pushers.yt_latencies.lib.datetime.datetime", new=MockDatetime)


@pytest.mark.parametrize("nodes", [
    [mocks.Node("path/table1", "2018-10-05T11:00:00.000000Z")],
    [mocks.Node("path/table1", "2018-10-05T11:00:00.000000Z"), mocks.Node("path/table2", "2018-10-05T10:00:10.000000Z")],
    [mocks.Node("path/table1", "2018-10-05T11:00:00.000000Z"), mocks.Node("path/map/table2", "2018-10-05T10:00:10.000000Z")],
    [mocks.Node("path", "2018-10-05T11:00:00.000000Z", "map_node")],
])
def test_get_map_node_metrics(nodes, mock_datetime):
    with mock_datetime:
        return lib.get_map_node_metrics(mocks.MockYtClient(nodes), "path")


@pytest.mark.parametrize("nodes", [
    [mocks.Node("table", "2018-10-05T11:00:00.000000Z")],
])
def test_get_node_metrics(nodes, mock_datetime):
    with mock_datetime:
        return lib.get_node_metrics(mocks.MockYtClient(nodes), "table")


def test_get(mock_datetime):
    nodes = [
        mocks.Node("dmp-test/fresh/ext_id_bindings/t1", "2018-10-05T10:00:00.000000Z"),
        mocks.Node("dmp-test/fresh/ext_id_bindings/t2", "2018-10-05T11:00:00.000000Z"),
        mocks.Node("dmp-test/fresh/meta/t1", "2018-10-04T11:00:00.000000Z"),
        mocks.Node("dmp-test/fresh/meta/t2", "2018-09-05T11:00:00.000000Z"),
        mocks.Node("dmp-test/fresh/cookies_matches/t1", "2017-10-05T11:00:00.000000Z"),
        mocks.Node("dmp-test/fresh/cookies_matches/t2", "2018-10-05T10:40:00.000000Z"),
    ]

    nodes_to_get = [
        "dmp-test/fresh/ext_id_bindings",
        "dmp-test/fresh/meta",
        "dmp-test/fresh/cookies_matches",
        "dmp-test/fresh/cookies_matches/t2",
    ]

    with mock_datetime:
        client = mocks.MockYtClient(nodes)
        return {node: lib.get_metrics(client, node) for node in nodes_to_get}
