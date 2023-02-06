import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)
from crypta.lookalike.proto.mode_pb2 import ModeValue
from crypta.lookalike.proto.segment_meta_entry_pb2 import TSegmentMetaEntry
from crypta.lookalike.proto.user_segments_pb2 import TUserSegments
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.lookalike.services.user_segments_exporter.proto.export_user_segments_job_config_pb2 import TExportUserSegmentsJobConfig

VERSION = "1584085850"

YT_NODE_NAMES = TYtNodeNames()

TIME_0 = "1590000000"
TIME_1 = "1590000001"


def get_config_file(yt_stuff, mode):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/user_segments_exporter/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "scope": "direct",
            "mode": mode,
        },
    )

    return config_file_path


def get_export_tests():
    return [
        tests.Diff(),
        tests.AttrsEquals({"sorted": True, "sorted_by": ["SegmentType", "SegmentID"]}),
    ]


def get_fresh_test(mode):
    if mode == ModeValue.NEW:
        return [tests.IsAbsent()]
    return [tests.TableIsNotChanged()]


def get_export_path(mode):
    return YT_NODE_NAMES.FreshLalExportTable if mode == ModeValue.NEW else YT_NODE_NAMES.LalExportTable


@pytest.mark.parametrize("mode", [
    pytest.param(ModeValue.NEW, id="new"),
    pytest.param(ModeValue.ALL, id="all"),
])
def test_basic(yt_stuff, mode):
    config_file = get_config_file(yt_stuff, mode)
    config = yaml_config.parse_config(TExportUserSegmentsJobConfig, config_file)

    def get_versioned_path(node):
        return os.path.join(config.VersionsDir, VERSION, node)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/user_segments_exporter/bin/crypta-lookalike-user-segments-exporter"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable(
                    "user_segments.yson",
                    get_versioned_path(YT_NODE_NAMES.UserSegmentsTable),
                    on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TUserSegments)})),
                [tests.TableIsNotChanged(), tests.DiffUserAttrs()]
            ),
            (
                tables.YsonTable(
                    "segment_metas.yson",
                    get_versioned_path(YT_NODE_NAMES.SegmentMetasTable),
                    on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TSegmentMetaEntry)})),
                [tests.TableIsNotChanged(), tests.DiffUserAttrs()]
            ),
            (
                tables.YsonTable(
                    "fresh_user_segments_0.yson",
                    os.path.join(get_versioned_path(YT_NODE_NAMES.FreshUserSegmentsDir), TIME_0),
                    on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TUserSegments)})),
                get_fresh_test(mode)
            ),
            (
                tables.YsonTable(
                    "fresh_segment_metas_0.yson",
                    os.path.join(get_versioned_path(YT_NODE_NAMES.FreshMetasDir), TIME_0),
                    on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TSegmentMetaEntry)})),
                get_fresh_test(mode)
            ),
            (
                tables.YsonTable(
                    "fresh_user_segments_1.yson",
                    os.path.join(get_versioned_path(YT_NODE_NAMES.FreshUserSegmentsDir), TIME_1),
                    on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TUserSegments)})),
                [tests.TableIsNotChanged(), tests.DiffUserAttrs()]
            ),
            (
                tables.YsonTable(
                    "fresh_segment_metas_1.yson",
                    os.path.join(get_versioned_path(YT_NODE_NAMES.FreshMetasDir), TIME_1),
                    on_write=tables.OnWrite(attributes={"schema": schema_utils.get_schema_from_proto(TSegmentMetaEntry)})),
                [tests.TableIsNotChanged(), tests.DiffUserAttrs()]
            ),
        ],
        output_tables=[
            (cypress.CypressNode(config.ExportDir), tests.TestNodesInMapNode(get_export_tests(), tag="audience_user_segments")),
            (tables.YsonTable(get_export_path(mode), get_versioned_path(get_export_path(mode)), yson_format="pretty"), get_export_tests()),
            (cypress.CypressNode(config.ErrorsDir), tests.TestNodesInMapNode([tests.Diff()], tag="errors")),
            (cypress.CypressNode(config.ExportLogDir), tests.TestNodesInMapNode([tests.Diff()], tag="log")),
        ],
        env={"YT_TOKEN": "FAKE", time_utils.CRYPTA_FROZEN_TIME_ENV: "1584543040"},
    )
