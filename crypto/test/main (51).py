import pytest
import os
import yatest.common

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
from crypta.lookalike.proto.segment_embedding_pb2 import TSegmentEmbedding
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.lookalike.services.index_builder.proto.index_builder_config_pb2 import TIndexBuilderConfig


VERSION = "1584085850"

YT_NODE_NAMES = TYtNodeNames()

TIME_1 = "1590000000"
TIME_2 = "1590000001"


def get_config_file(yt_stuff, mode):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/index_builder/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "mode": mode,
        },
    )

    return config_file_path


def get_versioned_path(config, node):
    return os.path.join(config.VersionsDir, VERSION, node)


def get_output_tables(config, mode):
    output_tables = []

    index_file = YT_NODE_NAMES.IndexFile
    data_file = YT_NODE_NAMES.DataFile
    labels_file = YT_NODE_NAMES.LabelsFile

    if mode == ModeValue.NEW:
        index_file = os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_1, YT_NODE_NAMES.IndexFile)
        data_file = os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_1, YT_NODE_NAMES.DataFile)
        labels_file = os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_1, YT_NODE_NAMES.LabelsFile)

        output_tables = [
            (files.YtFile(
                "segments_index_2",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_2, YT_NODE_NAMES.IndexFile)),
                on_write=files.OnWrite()
            ), tests.IsAbsent()),
            (files.YtFile(
                "segments_data_2",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_2, YT_NODE_NAMES.DataFile)),
                on_write=files.OnWrite()
            ), tests.IsAbsent()),
            (files.YtFile(
                "segments_labels_2",
                get_versioned_path(config, os.path.join(YT_NODE_NAMES.FreshFilesDir, TIME_2, YT_NODE_NAMES.LabelsFile)),
                on_write=files.OnWrite()
            ), tests.IsAbsent()),
        ]

    return output_tables + [
        (files.YtFile("segments_index", get_versioned_path(config, index_file), on_write=files.OnWrite()), tests.Diff()),
        (files.YtFile("segments_data", get_versioned_path(config, data_file), on_write=files.OnWrite()), tests.Diff()),
        (files.YtFile("segments_labels", get_versioned_path(config, labels_file), on_write=files.OnWrite()), tests.Diff()),
    ]


def get_fresh_tests(mode):
    if mode == ModeValue.NEW:
        return [tests.IsAbsent()]
    return [tests.TableIsNotChanged(), tests.DiffUserAttrs()]


@pytest.mark.parametrize("mode", [
    pytest.param(ModeValue.NEW, id="new"),
    pytest.param(ModeValue.ALL, id="all"),
])
def test_basic(yt_stuff, mode):
    config_file = get_config_file(yt_stuff, mode)
    config = yaml_config.parse_config(TIndexBuilderConfig, config_file)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/index_builder/bin/crypta-lookalike-index-builder"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                "segment_embeddings.yson",
                get_versioned_path(config, YT_NODE_NAMES.SegmentEmbeddingsTable),
                on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TSegmentEmbedding)})
            ), [tests.TableIsNotChanged(), tests.DiffUserAttrs()]),
            (tables.YsonTable(
                "fresh_segment_embeddings_1.yson",
                os.path.join(get_versioned_path(config, YT_NODE_NAMES.FreshSegmentEmbeddingsDir), TIME_1),
                on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TSegmentEmbedding)})
            ), get_fresh_tests(mode)),
            (tables.YsonTable(
                "fresh_segment_embeddings_2.yson",
                os.path.join(get_versioned_path(config, YT_NODE_NAMES.FreshSegmentEmbeddingsDir), TIME_2),
                on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TSegmentEmbedding)})
            ), [tests.TableIsNotChanged(), tests.DiffUserAttrs()]),
        ],
        output_tables=get_output_tables(config, mode),
        env={"LOCAL_YT_SERVER": yt_stuff.get_server(), time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION},
    )


def test_all_processed(yt_stuff):
    config_file = get_config_file(yt_stuff, "ALL")
    config = yaml_config.parse_config(TIndexBuilderConfig, config_file)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/index_builder/bin/crypta-lookalike-index-builder"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                "segment_embeddings.yson",
                get_versioned_path(config, YT_NODE_NAMES.SegmentEmbeddingsTable),
                on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TSegmentEmbedding), "last_processed": "2020-03-13"})
            ), [tests.TableIsNotChanged(), tests.DiffUserAttrs()]),
        ],
        output_tables=[],
        env={"LOCAL_YT_SERVER": yt_stuff.get_server(), time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION},
    )
