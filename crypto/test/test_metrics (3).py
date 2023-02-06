import pytest
import yatest.common
from yt import yson

import crypta.spine.pushers.yt_sizes.lib as metrics
from crypta.lib.python.yt.test_helpers import tables, tests


DATA_PATH = yatest.common.test_source_path("data")


@pytest.mark.parametrize(
    "node,attribute,default,expected_result",
    [
        ["//xxx/yyy/zzz", "www", None, 1],
        ["//xxx/yyy/zzz", "www", 4, 1],
        ["//xxx/yyy/aaa", "www", None, None],
        ["//xxx/yyy/aaa", "www", 4, 4],
        ["//xxx/yyy/zzz", "bbb", None, None],
        ["//xxx/yyy/zzz", "bbb", 4, 4],
    ],
)
def test_get_attribute(yt_stuff, node, attribute, default, expected_result):
    attributes = {"www": 1}
    yt_client = yt_stuff.get_yt_client()
    yt_client.create("table", path="//xxx/yyy/zzz", recursive=True, attributes=attributes)
    assert expected_result == metrics.get_attribute(yt_client, node, attribute, default)


def test_get_metrics_table(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_name = "xxx/yyy/zzz"
    result = tests.yt_test_func(
        yt_client,
        lambda: metrics.get_metrics(yt_client, table_name),
        data_path=DATA_PATH,
        return_result=True,
        input_tables=[(tables.YsonTable("test_get_metrics.input.yson", table_name), tests.TableIsNotChanged())],
    )

    expected_result = [
        {
            "disk_space": yt_client.get("//xxx/yyy/zzz/@resource_usage")["disk_space"],
            "chunk_count": yt_client.get("//xxx/yyy/zzz/@resource_usage")["chunk_count"],
            "node_count": yt_client.get("//xxx/yyy/zzz/@resource_usage")["node_count"],
            "row_count": yt_client.get("//xxx/yyy/zzz/@row_count"),
        }
    ]
    assert expected_result == result


def test_get_metrics_dynamic_table(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_name = "xxx/yyy/zzz"

    schema = yson.YsonList(
        [
            dict(name="xxx", type="int64", required=True, sort_order="ascending"),
            dict(name="yyy", type="int64", required=True),
        ]
    )
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = True

    result = tests.yt_test_func(
        yt_client,
        lambda: metrics.get_metrics(yt_client, table_name),
        data_path=DATA_PATH,
        return_result=True,
        input_tables=[
            (
                tables.get_yson_table_with_schema("test_get_metrics.input.yson", table_name, schema, dynamic=True),
                tests.TableIsNotChanged(),
            )
        ],
    )

    expected_result = [
        {
            "disk_space": yt_client.get("//xxx/yyy/zzz/@resource_usage")["disk_space"],
            "chunk_count": yt_client.get("//xxx/yyy/zzz/@resource_usage")["chunk_count"],
            "node_count": yt_client.get("//xxx/yyy/zzz/@resource_usage")["node_count"],
            "unmerged_row_count": yt_client.get("//xxx/yyy/zzz/@unmerged_row_count"),
        }
    ]
    assert expected_result == result


def test_get_metrics_map_node(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    table_name = "xxx/yyy/zzz"
    result = tests.yt_test_func(
        yt_client,
        lambda: metrics.get_metrics(yt_client, "xxx/yyy"),
        data_path=DATA_PATH,
        return_result=True,
        input_tables=[(tables.YsonTable("test_get_metrics.input.yson", table_name), tests.TableIsNotChanged())],
    )

    expected_result = [
        {
            "disk_space": yt_client.get("//xxx/yyy/@recursive_resource_usage")["disk_space"],
            "chunk_count": yt_client.get("//xxx/yyy/@recursive_resource_usage")["chunk_count"],
            "node_count": yt_client.get("//xxx/yyy/@recursive_resource_usage")["node_count"],
        }
    ]
    assert expected_result == result


def test_get_metrics_map_node_recursive(yt_stuff):
    yt_client = yt_stuff.get_yt_client()

    schema = yson.YsonList(
        [
            dict(name="xxx", type="int64", required=True, sort_order="ascending"),
            dict(name="yyy", type="int64", required=True),
        ]
    )
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = True

    result = tests.yt_test_func(
        yt_client,
        lambda: list(metrics.get_recursive_metrics(yt_client, "xxx/yyy")),
        data_path=DATA_PATH,
        return_result=True,
        input_tables=[
            (tables.YsonTable("test_get_metrics.input.yson", "xxx/yyy/zzz"), tests.TableIsNotChanged()),
            (
                tables.get_yson_table_with_schema("test_get_metrics.input.yson", "xxx/yyy/aaa", schema, dynamic=True),
                tests.TableIsNotChanged(),
            ),
        ],
    )

    expected_result = [
        [
            (
                "//xxx/yyy/aaa",
                {
                    "disk_space": yt_client.get("//xxx/yyy/aaa/@resource_usage")["disk_space"],
                    "chunk_count": yt_client.get("//xxx/yyy/aaa/@resource_usage")["chunk_count"],
                    "node_count": yt_client.get("//xxx/yyy/aaa/@resource_usage")["node_count"],
                    "unmerged_row_count": yt_client.get("//xxx/yyy/aaa/@unmerged_row_count"),
                },
            ),
            (
                "//xxx/yyy/zzz",
                {
                    "disk_space": yt_client.get("//xxx/yyy/zzz/@resource_usage")["disk_space"],
                    "chunk_count": yt_client.get("//xxx/yyy/zzz/@resource_usage")["chunk_count"],
                    "node_count": yt_client.get("//xxx/yyy/zzz/@resource_usage")["node_count"],
                    "row_count": yt_client.get("//xxx/yyy/zzz/@row_count"),
                },
            ),
        ]
    ]
    assert expected_result == result
