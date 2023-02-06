import os

import pytest
import yatest.common
from yt import yson

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.lookalike.proto.mode_pb2 import ModeValue
from crypta.lookalike.proto.user_embedding_pb2 import TUserEmbedding
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.lookalike.services.segmentator.proto.make_segments_job_config_pb2 import TMakeSegmentsJobConfig

VERSION = "1584085850"

YT_NODE_NAMES = TYtNodeNames()

TIME_0 = "1590000000"
TIME_1 = "1590000001"


def get_user_segments_schema(mode):
    schema = [
        {"name": "user_id", "type": "uint64", "required": True},
        {"name": "segments", "type": "any", "required": True, 'type_v3': {'item': 'uint64', 'type_name': 'list'}},
        {"name": "scores", "type": "any", "required": True, 'type_v3': {'item': 'double', 'type_name': 'list'}},
    ]

    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = False
    return schema


def get_config_file(yt_stuff, mode):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/segmentator/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "scope": "direct",
            "disable_avx_in_dot_product": True,
            "mode": mode,
        },
    )

    return config_file_path


def get_output_tables(config, mode):
    if mode == ModeValue.ALL:
        table = tables.YsonTable("user_segments.yson", os.path.join(config.VersionsDir, VERSION, YT_NODE_NAMES.UserSegmentsTable), yson_format="pretty")
    else:
        table = tables.YsonTable("fresh_user_segments.yson", os.path.join(config.VersionsDir, VERSION, YT_NODE_NAMES.FreshUserSegmentsDir, TIME_0), yson_format="pretty")
    return [(table, [tests.Diff(), tests.SchemaEquals(get_user_segments_schema(mode))])]


def get_fresh_tests(mode):
    if mode == ModeValue.NEW:
        return [tests.IsAbsent()]
    return [tests.Exists(), tests.DiffUserAttrs()]


def get_versioned_path(config, node):
    return os.path.join(config.VersionsDir, VERSION, node)


@pytest.mark.parametrize("mode", [
    pytest.param(ModeValue.NEW, id="new"),
    pytest.param(ModeValue.ALL, id="all"),
])
def test_basic(yt_stuff, mode):
    config_file = get_config_file(yt_stuff, mode)
    config = yaml_config.parse_config(TMakeSegmentsJobConfig, config_file)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/segmentator/bin/crypta-lookalike-segmentator"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                "user_embeddings.yson",
                get_versioned_path(config, YT_NODE_NAMES.UserEmbeddingsTable),
                on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TUserEmbedding)})
            ), [tests.TableIsNotChanged(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "segments_index",
                get_versioned_path(config, YT_NODE_NAMES.IndexFile),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "segments_data",
                get_versioned_path(config, YT_NODE_NAMES.DataFile),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "segments_labels",
                get_versioned_path(config, YT_NODE_NAMES.LabelsFile),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "fresh_segments_index_0",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_0, YT_NODE_NAMES.IndexFile)),
                on_write=files.OnWrite()),
             get_fresh_tests(mode)),
            (files.YtFile(
                "fresh_segments_data_0",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_0, YT_NODE_NAMES.DataFile)),
                on_write=files.OnWrite()),
             get_fresh_tests(mode)),
            (files.YtFile(
                "fresh_segments_labels_0",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_0, YT_NODE_NAMES.LabelsFile)),
                on_write=files.OnWrite()),
             get_fresh_tests(mode)),
            (files.YtFile(
                "fresh_segments_index_1",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_1, YT_NODE_NAMES.IndexFile)),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "fresh_segments_data_1",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_1, YT_NODE_NAMES.DataFile)),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "fresh_segments_labels_1",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_1, YT_NODE_NAMES.LabelsFile)),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
        ],
        output_tables=get_output_tables(config, mode),
        env={"LOCAL_YT_SERVER": yt_stuff.get_server(), time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION},
    )


def test_all_processed(yt_stuff):
    config_file = get_config_file(yt_stuff, "ALL")
    config = yaml_config.parse_config(TMakeSegmentsJobConfig, config_file)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/segmentator/bin/crypta-lookalike-segmentator"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                "user_embeddings.yson",
                get_versioned_path(config, YT_NODE_NAMES.UserEmbeddingsTable),
                on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TUserEmbedding), "last_processed": "2020-03-13"})
            ), [tests.TableIsNotChanged(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "segments_index",
                get_versioned_path(config, YT_NODE_NAMES.IndexFile),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "segments_data",
                get_versioned_path(config, YT_NODE_NAMES.DataFile),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
            (files.YtFile(
                "segments_labels",
                get_versioned_path(config, YT_NODE_NAMES.LabelsFile),
                on_write=files.OnWrite()),
             [tests.Exists(), tests.DiffUserAttrs()]),
        ],
        output_tables=[],
        env={"LOCAL_YT_SERVER": yt_stuff.get_server(), time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION},
    )
