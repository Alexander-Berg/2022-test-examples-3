from __future__ import print_function
from textwrap import dedent

import pytest
import mock
import yql.library.embedded.python.run as embedded
from yql_utils import yql_binary_path
from crypta.lib.python.yql_runner.tests import load_fixtures, clean_up


def load_fixtures_impl(yt):
    attrib1 = {
        "_yql_row_spec": {
            "SortDirections": [1, 1],
            "UniqueKeys": False,
            "SortedBy": ["id", "id_type"],
            "StrictSchema": True,
            "SortMembers": ["id", "id_type"],
            "SortedByTypes": [["OptionalType", ["DataType", "String"]], ["OptionalType", ["DataType", "String"]]],
            "Type": [
                "StructType",
                [
                    ["id", ["OptionalType", ["DataType", "String"]]],
                    ["id_type", ["OptionalType", ["DataType", "String"]]],
                    ["source", ["OptionalType", ["DataType", "String"]]],
                ],
            ],
        },
        "schema": [
            {"sort_order": "ascending", "type": "string", "name": "id", "required": False},
            {"sort_order": "ascending", "type": "string", "name": "id_type", "required": False},
            {"required": False, "type": "string", "name": "source"},
        ],
    }

    attrib2 = {
        "_yql_row_spec": {
            "SortDirections": [1, 1],
            "UniqueKeys": False,
            "SortedBy": ["id", "id_type"],
            "StrictSchema": True,
            "SortMembers": ["id", "id_type"],
            "SortedByTypes": [["OptionalType", ["DataType", "String"]], ["DataType", "String"]],
            "Type": [
                "StructType",
                [
                    ["id", ["OptionalType", ["DataType", "String"]]],
                    ["id_type", ["DataType", "String"]],
                    ["source", ["DataType", "String"]],
                ],
            ],
        },
        "schema": [
            {"sort_order": "ascending", "type": "string", "name": "id", "required": False},
            {"sort_order": "ascending", "type": "string", "name": "id_type", "required": True},
            {"required": True, "type": "string", "name": "source"},
        ],
    }

    data1 = [
        {"source": "Heuristic shared desktop yuid", "id": "601826891455541119", "id_type": "yandexuid"},
        {"source": "Heuristic shared desktop yuid", "id": "601826891455541120", "id_type": "yandexuid"},
    ]
    data2 = [
        {"source": "Yandex Drive", "id": "00002907a397aea15dbfbdcf0472a112", "id_type": "mm_device_id"},
        {"source": "Yandex Drive", "id": "06a3907a6eb48f9ae525d66555555555", "id_type": "uuid"},
        {"source": "Yandex Drive", "id": "06a3907a6eb48f9ae525d6f666666666", "id_type": "uuid"},
    ]

    yt.yt_client.create("table", "//home/input_2", recursive=True, attributes=attrib1)
    yt.yt_client.write_table("//home/input_2", data1, format="json")

    yt.yt_client.create("table", "//home/input_1", recursive=True, attributes=attrib2)
    yt.yt_client.write_table("//home/input_1", data2, format="json")


@pytest.mark.skip("disable test")
@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@clean_up()
@load_fixtures(load_fixtures_impl)
def test_embeded_yql(yt):
    yt_clusters = [{"name": "ytcluster", "cluster": "localhost:%d" % yt.yt_proxy_port}]
    factory = embedded.OperationFactory(
        yt_clusters=yt_clusters,
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=yql_binary_path("yql/udfs"),
    )
    query = dedent(
        """\
        USE YtCluster;

        INSERT INTO `//home/output` WITH TRUNCATE
        SELECT
            id,
            id_type
        FROM CONCAT(
            `//home/input_1`,
            `//home/input_2`
        )
        GROUP BY id, id_type
        ORDER BY id, id_type;
    """
    )

    factory.run(query, syntax_version=1).yson_result()


@pytest.mark.xfail
@mock.patch.dict("os.environ", {"YT_TOKEN": "FAKE", "ENV_TYPE": "FAKE"})
@clean_up()
def test_embeded_yql_does_not_exist(yt):
    yt_clusters = [{"name": "ytcluster", "cluster": "localhost:%d" % yt.yt_proxy_port}]
    factory = embedded.OperationFactory(
        yt_clusters=yt_clusters,
        mrjob_binary=yql_binary_path("yql/tools/mrjob/mrjob"),
        udf_resolver_binary=yql_binary_path("yql/tools/udf_resolver/udf_resolver"),
        udfs_dir=yql_binary_path("yql/udfs"),
    )
    query = dedent(
        """\
        USE ytcluster;

        INSERT INTO `//home/crypta/team/mskorokhod/table_exists` WITH TRUNCATE
        SELECT 1 AS value;
    """
    )

    factory.run(query, syntax_version=1).yson_result()

    query = dedent(
        """\
        USE ytcluster;
        PRAGMA Warning("error", "*");

        SELECT * FROM EACH(AsList(
            "//home/crypta/team/mskorokhod/table_does_not_exists",
            "//home/crypta/team/mskorokhod/table_exists"
        ));
    """
    )

    factory.run(query, syntax_version=1).yson_result()
