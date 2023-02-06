import json
import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.lookalike.lib.python import test_utils
from crypta.lookalike.proto.mode_pb2 import ModeValue
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.lookalike.services.segment_dssm_applier.proto.config_pb2 import TConfig


VERSION = "1584085850"

YT_NODE_NAMES = TYtNodeNames()

MAPPING_LOCAL_PATH = "./mapping.json"


def get_config_file(yt_stuff, logbroker_port, mode):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/segment_dssm_applier/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "lb_url": "localhost",
            "lb_topic": "default-topic",
            "lb_port": logbroker_port,
            "scope": "direct",
            "input_table": "//home/crypta/qa/direct",
            "mode": mode,
        },
    )

    return config_file_path


def get_versioned_path(config, node):
    return os.path.join(config.VersionsDir, VERSION, node)


def get_output_tables(config, mode):
    output_tables = [(
        tables.YsonTable(
            "errors.yson",
            os.path.join(config.ErrorsDir, VERSION),
            yson_format="pretty"
        ),
        [tests.IsAbsent()],
    )]

    if mode == ModeValue.ALL:
        output_tables = [
            (
                tables.YsonTable(
                    "segment_embeddings.yson",
                    get_versioned_path(config, YT_NODE_NAMES.SegmentEmbeddingsTable),
                    yson_format="pretty",
                    on_read=test_utils.embeddings_on_read()
                ),
                [tests.Diff()],
            ),
            (
                tables.YsonTable(
                    "segment_metas.yson",
                    get_versioned_path(config, YT_NODE_NAMES.SegmentMetasTable),
                    yson_format="pretty"
                ),
                [tests.Diff()],
            ),
        ]
    elif mode == ModeValue.NEW:
        output_tables = [
            (
                tables.YsonTable(
                    "fresh_segment_embeddings.yson",
                    os.path.join(get_versioned_path(config, YT_NODE_NAMES.FreshSegmentEmbeddingsDir), VERSION),
                    yson_format="pretty",
                    on_read=test_utils.embeddings_on_read()
                ),
                [tests.Diff()],
            ),
            (
                tables.YsonTable(
                    "fresh_segment_metas.yson",
                    os.path.join(get_versioned_path(config, YT_NODE_NAMES.FreshMetasDir), VERSION),
                    yson_format="pretty"
                ),
                [tests.Diff()],
            ),
        ]
    return output_tables


@pytest.mark.parametrize("mode", [
    pytest.param(ModeValue.NEW, id="new"),
    pytest.param(ModeValue.ALL, id="all"),
])
def test_basic(yt_stuff, consumer, logbroker_port, mode):
    config_file = get_config_file(yt_stuff, logbroker_port, mode)
    config = yaml_config.parse_config(TConfig, config_file)

    results = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/segment_dssm_applier/bin/crypta-lookalike-segment-dssm-applier"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("lals.yson", config.LalsTable, on_write=test_utils.lals_on_write()), tests.TableIsNotChanged()),
            (files.YtFile(yatest.common.work_path("dssm_lal_model.applier"), get_versioned_path(config, YT_NODE_NAMES.DssmModelFile), on_write=files.OnWrite()), tests.Exists()),
            (files.YtFile(yatest.common.work_path("segments_dict"), get_versioned_path(config, YT_NODE_NAMES.SegmentsDictFile), on_write=files.OnWrite()), tests.Exists()),
        ],
        output_tables=get_output_tables(config, mode),
        env={"YT_TOKEN": "FAKE", time_utils.CRYPTA_FROZEN_TIME_ENV: VERSION, "MAPPING_LOCAL_PATH": MAPPING_LOCAL_PATH},
    )

    with open(MAPPING_LOCAL_PATH, "r") as file:
        mapping = json.load(file)

    for key in mapping:
        mapping[key] = sorted(mapping[key])
    os.remove(MAPPING_LOCAL_PATH)

    return {
        "yt_results": results,
        "data_written": sorted(consumer_utils.read_all(consumer, timeout=30)),
        "mapping": mapping,
    }
