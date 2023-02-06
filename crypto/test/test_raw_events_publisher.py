import os

from google.protobuf import json_format

import yatest.common
import yt.wrapper as yt
import yt.yson.convert as yson_convert

from crypta.lib.python import (
    templater,
    time_utils,
)
from crypta.lib.python.yt.test_helpers import (
    replicated_tables,
    tables,
    tests,
)
from crypta.lib.python.yt.tm_utils.test_with_tm import TestWithTm
from crypta.styx.services.common.test_utils.styx_db_record_pb2 import TStyxDbRecord


CONFIG_TEMPLATE = "crypta/styx/services/raw_events_publisher/bundle/config.yaml"
FROZEN_TIME = "1650000000"


def render_config_file(template_path, parameters, test_subdir=None, file_name="config.yaml"):
    template_path = yatest.common.source_path(template_path)

    working_dir = yatest.common.test_output_path(test_subdir)
    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, file_name)
    templater.render_file(template_path, config_path, parameters)

    return config_path


def get_config_path(src_cluster, master_table, src_table, dst_cluster, dst_dir, yt_tmp_dir):
    return render_config_file(
        CONFIG_TEMPLATE,
        {
            "master_cluster": src_cluster,
            "master_table": master_table,
            "src_cluster": src_cluster,
            "src_pool": "pool",
            "src_table": src_table,
            "dst_cluster": dst_cluster,
            "dst_dir": dst_dir,
            "yt_tmp_dir": yt_tmp_dir,
        },
    )


def styx_db_row_transformer(proto_class):
    def row_transformer(row):
        proto = json_format.ParseDict(yson_convert.yson_to_json(row), proto_class())
        return {
            "key": proto.Key,
            "value": proto.Value.SerializeToString(),
        }

    return row_transformer


class TestRawEventsPublisher(TestWithTm):
    def test_raw_events_publisher(self):
        src_dir = self.get_subdir_path("src")
        master_table_path = yt.ypath_join(src_dir, "master")
        src_table_path = yt.ypath_join(src_dir, "replica")
        dst_dir = self.get_subdir_path("dst")
        dst_table = yt.ypath_join(dst_dir, "as-table")
        yt_tmp_dir = self.get_subdir_path("tmp")

        config_path = get_config_path(self.first_cluster, master_table_path, src_table_path, self.second_cluster, dst_dir, yt_tmp_dir)

        env = self.get_tm_env()
        env[time_utils.CRYPTA_FROZEN_TIME_ENV] = FROZEN_TIME

        return tests.yt_test(
            self.first_cluster_client,
            binary=yatest.common.binary_path("crypta/styx/services/raw_events_publisher/bin/crypta-styx-raw-events-publisher"),
            args=["--config", config_path],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (
                    replicated_tables.ReplicatedDynamicYsonTable(
                        "input.yson",
                        self.first_cluster_client,
                        master_table_path,
                        self.first_cluster_client,
                        src_table_path,
                        on_read=tables.OnRead(styx_db_row_transformer(TStyxDbRecord)),
                    ),
                    tests.TableIsNotChanged()
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable("output.yson", dst_table, yson_format="pretty"),
                    tests.Diff(yt_client=self.second_cluster_client)
                ),
            ],
            env=env,
        )
